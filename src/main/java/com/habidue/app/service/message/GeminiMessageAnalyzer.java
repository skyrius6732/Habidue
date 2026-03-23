package com.habidue.app.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.message.AiAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiMessageAnalyzer implements MessageAnalyzer {

    @Value("${gemini.api.key:}") // 키가 없으면 빈 문자열 주입
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // [유지] 사용자가 확인한 모델명 사용
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @Override
    public AiAnalysisResult analyze(String content) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("${")) {
            return AiAnalysisResult.safe();
        }

        try {
            log.info("🤖 [Single] Gemini 분석 요청 시작...");
            
            String prompt = String.format(
                "당신은 건전한 커뮤니티를 관리하는 모더레이터 AI입니다. 다음 쪽지 내용을 분석하여 '명백한 악성 메시지'인지 판별하세요.\n\n" +
                "[커뮤니티 성격]: 공공주택 공고, 청약 정보 공유 및 일상 소통 커뮤니티\n" +
                "[분석 가이드라인]:\n" +
                "1. 단순히 '예비번호', '대기순번'을 묻거나 도움을 제안하는 내용은 정보 공유로 간주하여 '안전'으로 처리하세요.\n" +
                "2. 명백한 패륜적 욕설, 성희롱, 지속적인 괴롭힘, 금전 갈취 시도만 '위험'으로 분류하세요.\n" +
                "3. 판단이 모호한 경우 최대한 '안전(false)'으로 응답하여 유저 간 소통을 방해하지 마세요.\n" +
                "4. 비방이 아닌 일상적인 대화나 질문은 score를 0.3 이하로 낮게 책정하세요.\n\n" +
                "반드시 아래 JSON 형식으로만 응답하세요.\n" +
                "{\"score\": 위험도(0.0~1.0), \"reason\": \"이유\", \"shouldBlock\": true/false}\n\n" +
                "내용: \"%s\"", content
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_URL + apiKey, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                log.info("📩 [Single] Gemini 응답 수신: {}", body);
                
                JsonNode root = objectMapper.readTree(body);
                String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                JsonNode result = objectMapper.readTree(jsonText.trim());

                return AiAnalysisResult.builder()
                        .score(result.path("score").asDouble())
                        .reason(result.path("reason").asText())
                        .shouldBlock(result.path("shouldBlock").asBoolean())
                        .build();
            }
        } catch (Exception e) {
            log.error("❌ [Single] 분석 중 오류: {}", e.getMessage());
        }
        return AiAnalysisResult.safe();
    }

    @Override
    public String getName() { return "GEMINI_PRO"; }

    /**
     * 관리자용: 대화 맥락 전체 정밀 분석 (신고자/피신고자 정보 포함 및 문구 생성)
     */
    public Map<String, Object> analyzeContext(List<com.habidue.app.domain.message.Message> messages, Long targetMessageId, User reporterUser) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("${")) {
            return Map.of("error", "API 키 누락");
        }

        try {
            StringBuilder logBuilder = new StringBuilder();
            String suspectNickname = "피신고자";
            String reporterNickname = reporterUser != null ? reporterUser.getNickname() : "신고자";
            Long reporterId = reporterUser != null ? reporterUser.getId() : 0L;

            for (com.habidue.app.domain.message.Message m : messages) {
                boolean isTarget = m.getId().equals(targetMessageId);
                boolean isFromReporter = m.getSender().getId().equals(reporterId);
                
                String roleTag = isFromReporter ? "[신고자 본인]" : "[피신고자(가해의심)]";
                String targetTag = isTarget ? " <<🚨 실제 신고된 메시지 >>" : "";
                
                if (isTarget) suspectNickname = m.getSender().getNickname();
                
                logBuilder.append(String.format("%s %s (ID:%d): %s %s\n", 
                    roleTag, m.getSender().getNickname(), m.getSender().getId(), m.getContent(), targetTag));
            }

            String prompt = String.format(
                "당신은 전문 커뮤니티 분쟁 조정관입니다. 현재 [신고자: %s]가 [피신고자: %s]를 신고한 상황입니다.\n\n" +
                "[핵심 정보]\n" +
                "- 분석 타겟: '<<🚨 실제 신고된 메시지 >>' 표시가 붙은 피신고자의 발언\n" +
                "- 주의: 절대 신고자(%s)의 발언을 피신고자의 것으로 오해하여 신고자를 제재하는 결론을 내리지 마세요.\n\n" +
                "[대화 로그 분석 데이터]\n%s\n\n" +
                "[분석 및 출력 지침]\n" +
                "1. 피신고자(%s)가 보낸 '신고된 메시지'가 운영원칙을 위반했는지 맥락을 통해 판별하세요.\n" +
                "2. recommendedAction 결정: 'BLIND'(1단계), 'DELETE'(2단계), 'REJECT'(반려)\n" +
                "3. violationPoint는 시스템 메시지 중간에 삽입될 핵심 사유입니다. (예: '상대방에 대한 비하와 비속어 사용')\n" +
                " - 문맥에 맞게 '~ 사용', '~ 발언', '~ 행위' 등의 명사형으로 15자 이내로 작성하세요.\n\n" +
                "반드시 아래 JSON 형식으로만 응답하세요.\n" +
                "{\n" +
                "  \"score\": 위험도 수치(0.0~1.0),\n" +
                "  \"recommendedAction\": \"BLIND/DELETE/REJECT\",\n" +
                "  \"violationPoint\": \"피신고자 %s의 위반 사유\",\n" +
                "  \"summary\": \"대화 맥락 및 위반 정황 요약\"\n" +
                "}", 
                reporterNickname, suspectNickname, 
                reporterNickname, 
                logBuilder.toString(),
                suspectNickname, suspectNickname
            );

            log.info("🚀 [Context] 역할 고정 Gemini 분석 요청 발송 중...");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_URL + apiKey, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                log.info("✅ [Context] Gemini 응답 수신 성공!");
                
                JsonNode root = objectMapper.readTree(body);
                String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                
                // [시니어 조치] Markdown 코드 블록(```json ... ```) 제거 로직 추가
                String cleanedJson = jsonText.trim();
                if (cleanedJson.startsWith("```")) {
                    cleanedJson = cleanedJson.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
                }

                Map<String, Object> resultMap = objectMapper.readValue(cleanedJson, Map.class);
                log.info("📊 [Context] 분석 결과 파싱 성공: {}", resultMap);
                return resultMap;
            } else {
                log.error("❌ [Context] Gemini API 호출 에러 (상태코드: {})", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ [Context] 맥락 분석 중 예외 발생: {}", e.getMessage(), e);
        }

        return Map.of("error", "AI 분석 결과 파싱 중 오류가 발생했습니다. 로그를 확인하세요.");
    }
}
