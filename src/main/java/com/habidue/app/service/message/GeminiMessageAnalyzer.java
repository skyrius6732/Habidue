package com.habidue.app.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @Override
    public AiAnalysisResult analyze(String content) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("${")) {
            log.debug("Gemini API Key is missing. Skipping AI analysis.");
            return AiAnalysisResult.safe();
        }

        try {
            log.info("📢 Gemini AI 분석 시작: {}", content);
            
            String prompt = String.format(
                "커뮤니티 쪽지 내용을 분석하여 비방, 욕설, 가스라이팅, 스팸 여부를 판단하세요.\n" +
                "반드시 아래 JSON 형식으로만 응답하세요.\n" +
                "{\"score\": 위험도(0.0~1.0), \"reason\": \"판단 근거\", \"shouldBlock\": true/false}\n" +
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
                log.info("✅ Gemini 응답 수신: {}", body);
                
                JsonNode root = objectMapper.readTree(body);
                String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                JsonNode result = objectMapper.readTree(jsonText.trim());

                double score = result.path("score").asDouble();
                String reason = result.path("reason").asText();
                boolean shouldBlock = result.path("shouldBlock").asBoolean();

                log.info("📊 분석 결과 - Score: {}, Reason: {}", score, reason);

                return AiAnalysisResult.builder()
                        .score(score)
                        .reason(reason)
                        .shouldBlock(shouldBlock)
                        .build();
            } else {
                log.error("❌ Gemini API 호출 실패: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Gemini 분석 중 예외 발생: {}", e.getMessage(), e);
        }

        return AiAnalysisResult.safe();
    }

    @Override
    public String getName() {
        return "GEMINI_PRO";
    }
}
