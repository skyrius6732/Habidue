package com.habidue.app.controller.api;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.message.MessageRequestDto;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.service.user.UserService;
import com.habidue.app.config.oauth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final com.habidue.app.service.board.BoardReportService boardReportService;

    // [시니어 조치] UserPrincipal에서 User 엔티티를 안전하게 가져오는 헬퍼 메서드
    private User getAuthenticatedUser(UserPrincipal principal) {
        if (principal.getUser() != null) return principal.getUser();
        return userService.getUserById(principal.getId()); // ID로 조회 (JWT 인증 시 대응)
    }

    @PostMapping({"", "/{receiverId}"})
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable(value = "receiverId", required = false) Long receiverId,
            @ModelAttribute MessageRequestDto requestDto,
            HttpServletRequest request) {
        
        if (receiverId == null) {
            String paramId = request.getParameter("receiverId");
            if (paramId != null && !paramId.isEmpty()) receiverId = Long.valueOf(paramId);
        }

        if (receiverId == null) throw new IllegalArgumentException("수신자 정보를 확인할 수 없습니다.");
        
        String content = requestDto.getContent();
        List<MultipartFile> files = requestDto.getFiles();

        if (content == null || content.trim().isEmpty()) content = request.getParameter("content");
        if (files == null || files.isEmpty()) {
            if (request instanceof MultipartHttpServletRequest) files = ((MultipartHttpServletRequest) request).getFiles("files");
        }

        if ((content == null || content.trim().isEmpty()) && (files == null || files.isEmpty())) {
            throw new IllegalArgumentException("메시지 내용 또는 파일을 입력해주세요.");
        }
        
        messageService.sendMessage(userPrincipal.getId(), receiverId, content, files);
        return ApiResponse.success(null);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(messageService.getDailyMessageStatus(userPrincipal.getId()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Page<com.habidue.app.dto.message.MessageResponseDto>>> getMessageRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {
        User user = getAuthenticatedUser(userPrincipal);
        userService.updateUserOnlineStatus(user.getId());
        Page<Message> messages = messageService.getLatestMessagesGroupedByPartner(user, pageable);

        return ApiResponse.success(messages.map(m -> {
            User partner = (m.getSender() != null && m.getSender().getId().equals(user.getId())) ? m.getReceiver() : m.getSender();
            long unreadCount = (partner != null) ? messageService.countUnreadMessagesWithPartner(user, partner) : 0;
            boolean isOnline = (partner != null) && userService.isUserOnline(partner.getId());
            return com.habidue.app.dto.message.MessageResponseDto.from(m, unreadCount, isOnline);
        }));
    }

    @GetMapping("/conversation/{partnerId}")
    public ResponseEntity<ApiResponse<java.util.List<com.habidue.app.dto.message.MessageResponseDto>>> getConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long partnerId) {
        User user = getAuthenticatedUser(userPrincipal);
        userService.updateUserOnlineStatus(user.getId());
        java.util.List<Message> conversation = messageService.getConversation(user, partnerId);
        boolean isPartnerOnline = userService.isUserOnline(partnerId);

        return ApiResponse.success(conversation.stream()
                .map(m -> com.habidue.app.dto.message.MessageResponseDto.from(m, null, isPartnerOnline))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PatchMapping("/conversation/{partnerId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long partnerId) {
        messageService.markConversationAsRead(getAuthenticatedUser(userPrincipal), partnerId);
        return ApiResponse.success(null);
    }

    @PostMapping("/active")
    public ResponseEntity<ApiResponse<Void>> updateActiveStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.updateUserOnlineStatus(userPrincipal.getId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/conversation/{partnerId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long partnerId) {
        messageService.deleteConversation(getAuthenticatedUser(userPrincipal), partnerId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId) {
        messageService.deleteMessage(messageId, getAuthenticatedUser(userPrincipal));
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/edit")
    public ResponseEntity<ApiResponse<Void>> editMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId,
            @RequestBody java.util.Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("수정할 내용을 입력해주세요.");
        messageService.editMessage(messageId, getAuthenticatedUser(userPrincipal), content);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId) {
        messageService.readMessage(messageId, getAuthenticatedUser(userPrincipal));
        return ApiResponse.success(null);
    }

    @PostMapping("/{messageId}/report")
    public ResponseEntity<ApiResponse<Void>> reportMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long messageId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "부적절한 내용의 쪽지";
        messageService.reportMessage(messageId, getAuthenticatedUser(userPrincipal));
        com.habidue.app.dto.board.ReportRequestDto reportDto = new com.habidue.app.dto.board.ReportRequestDto();
        reportDto.setTargetId(messageId);
        reportDto.setTargetType(com.habidue.app.domain.board.ReportTargetType.MESSAGE);
        reportDto.setReason(reason);
        boardReportService.report(reportDto);
        return ApiResponse.success(null);
    }

    @PostMapping("/block/{blockedId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long blockedId) {
        messageService.blockUser(getAuthenticatedUser(userPrincipal), blockedId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/block/{blockedId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long blockedId) {
        messageService.unblockUser(getAuthenticatedUser(userPrincipal), blockedId);
        return ApiResponse.success(null);
    }

    @GetMapping("/block/list")
    @Transactional(readOnly = true) // [시니어 조치] LazyLoading 에러 방지
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<com.habidue.app.domain.message.UserBlock> blockedList = messageService.getBlockedUsers(getAuthenticatedUser(userPrincipal));
        List<Map<String, Object>> response = blockedList.stream().map(b -> {
            Map<String, Object> map = new java.util.HashMap<>();
            User blocked = b.getBlocked(); // 강제 초기화 유도
            map.put("id", blocked.getId());
            map.put("nickname", blocked.getNickname() != null ? blocked.getNickname() : blocked.getUsername());
            map.put("reason", b.getReason());
            map.put("isSystemBlock", b.isSystemBlock());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(response);
    }
}
