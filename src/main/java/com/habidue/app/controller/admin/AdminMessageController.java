package com.habidue.app.controller.admin;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.AdminMessageResponseDto;
import com.habidue.app.service.message.GeminiMessageAnalyzer;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final ReportRepository reportRepository;
    private final GeminiMessageAnalyzer geminiMessageAnalyzer;

    /**
     * 관리자 자신의 일일 쪽지 발송 횟수 초기화
     */
    @DeleteMapping("/daily-count")
    public ResponseEntity<ApiResponse<Void>> resetDailyCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        messageService.resetDailyMessageCount(userPrincipal.getId());
        return ApiResponse.success(null);
    }

    /**
     * 신고된 메시지 기반 대화 로그 조회 (DB에 저장된 AI 분석 결과 포함)
     */
    @GetMapping("/log/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMessageLog(@PathVariable Long messageId) {
        Message reportedMsg = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("메시지를 찾을 수 없습니다."));

        List<Message> logList = messageRepository.findConversationWithPartner(reportedMsg.getSender(), reportedMsg.getReceiver());
        List<AdminMessageResponseDto> messagesDto = logList.stream()
                .sorted(java.util.Comparator.comparing(Message::getCreatedAt)) // [시니어 조치] 시간순 정렬 추가
                .map(AdminMessageResponseDto::from)
                .collect(Collectors.toList());

        boolean isMutualReport = reportRepository.existsActiveReportByConversation(
                reportedMsg.getSender().getId(), 
                reportedMsg.getSender(), 
                reportedMsg.getReceiver()
        );

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("messages", messagesDto);
        response.put("isMutualReport", isMutualReport);
        
        if (reportedMsg.getAiAnalysis() != null && !reportedMsg.getAiAnalysis().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> aiResult = mapper.readValue(reportedMsg.getAiAnalysis(), Map.class);
                response.put("aiResult", aiResult);
                log.info("🎯 DB에 저장된 AI 분석 결과를 불러왔습니다. (ID: #{})", messageId);
            } catch (Exception e) {
                log.error("AI 데이터 파싱 실패: {}", e.getMessage());
            }
        }
        
        return ApiResponse.success(response);
    }

    /**
     * 신고된 메시지 AI 정밀 분석 및 DB 영구 저장
     */
    @PostMapping("/{messageId}/ai-analyze")
    @Transactional 
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiAnalyzeMessage(@PathVariable Long messageId) {
        Message target = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("메시지를 찾을 수 없습니다."));

        if (target.getAiAnalysis() != null && !target.getAiAnalysis().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> cached = mapper.readValue(target.getAiAnalysis(), Map.class);
                return ApiResponse.success(cached);
            } catch (Exception e) {}
        }

        User reporterUser = reportRepository.findAllByTargetIdAndTargetType(messageId, com.habidue.app.domain.board.ReportTargetType.MESSAGE)
                .stream().findFirst().map(com.habidue.app.domain.board.Report::getReporter).orElse(null);

        List<Message> conversation = messageRepository.findConversationWithPartner(target.getSender(), target.getReceiver());
        Map<String, Object> analysisResult = geminiMessageAnalyzer.analyzeContext(conversation, messageId, reporterUser);
        
        if (!analysisResult.containsKey("error")) {
            try {
                saveAnalysisResult(target, analysisResult);
                log.info("💾 AI 분석 결과를 DB에 영구 저장했습니다. (ID: #{})", messageId);
            } catch (Exception e) {
                log.error("⚠️ AI 결과 DB 저장 실패: {}", e.getMessage());
            }
        }
        
        return ApiResponse.success(analysisResult);
    }

    private void saveAnalysisResult(Message target, Map<String, Object> result) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonResult = mapper.writeValueAsString(result);
        Double score = Double.valueOf(result.getOrDefault("score", 0.0).toString());
        
        target.updateAiResult(score, jsonResult);
        messageRepository.saveAndFlush(target);
    }
}
