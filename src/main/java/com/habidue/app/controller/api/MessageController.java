package com.habidue.app.controller.api;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.message.MessageRequestDto;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    /**
     * 쪽지 발송 (ID가 있거나 없는 경우를 모두 매핑하여 404 방지)
     */
    @PostMapping({"", "/{receiverId}"})
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable(value = "receiverId", required = false) Long receiverId,
            @ModelAttribute MessageRequestDto requestDto,
            HttpServletRequest request) {
        
        log.info("==== [ 쪽지 발송 디버깅 ] ====");
        
        if (receiverId == null) {
            String paramId = request.getParameter("receiverId");
            if (paramId != null && !paramId.isEmpty()) {
                receiverId = Long.valueOf(paramId);
            }
        }

        if (receiverId == null) {
            log.error("🚨 수신자 ID 추출 실패!");
            throw new IllegalArgumentException("수신자 정보를 확인할 수 없습니다. 다시 시도해 주세요.");
        }
        
        String content = requestDto.getContent();
        List<MultipartFile> files = requestDto.getFiles();

        if (content == null || content.trim().isEmpty()) {
            content = request.getParameter("content");
        }
        
        if (files == null || files.isEmpty()) {
            if (request instanceof MultipartHttpServletRequest) {
                files = ((MultipartHttpServletRequest) request).getFiles("files");
            }
        }

        log.info("Sender: {}, Target: {}, Content: {}", userDetails.getUsername(), receiverId, content);

        if ((content == null || content.trim().isEmpty()) && (files == null || files.isEmpty())) {
            log.error("🚨 최종 데이터 추출 실패: Content와 File 모두 없음");
            throw new IllegalArgumentException("메시지 내용 또는 파일을 입력해주세요.");
        }
        
        User sender = userService.getUserByUsername(userDetails.getUsername());
        messageService.sendMessage(sender.getId(), receiverId, content, files);
        
        return ApiResponse.success(null);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ApiResponse.success(messageService.getDailyMessageStatus(user.getId()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Page<com.habidue.app.dto.message.MessageResponseDto>>> getMessageRooms(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        // 접속 상태 갱신 (메서드명 수정: setUserOnline -> updateUserOnlineStatus)
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long partnerId) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        // 접속 상태 갱신 (메서드명 수정: setUserOnline -> updateUserOnlineStatus)
        userService.updateUserOnlineStatus(user.getId());

        java.util.List<Message> conversation = messageService.getConversation(user, partnerId);
        boolean isPartnerOnline = userService.isUserOnline(partnerId);

        return ApiResponse.success(conversation.stream()
                .map(m -> com.habidue.app.dto.message.MessageResponseDto.from(m, null, isPartnerOnline))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/active")
    public ResponseEntity<ApiResponse<Void>> updateActiveStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        // 접속 상태 갱신 (메서드명 수정: setUserOnline -> updateUserOnlineStatus)
        userService.updateUserOnlineStatus(user.getId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/conversation/{partnerId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long partnerId) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.deleteConversation(user, partnerId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.deleteMessage(messageId, user);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/edit")
    public ResponseEntity<ApiResponse<Void>> editMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId,
            @RequestBody java.util.Map<String, String> body) {
        
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("수정할 내용을 입력해주세요.");
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.editMessage(messageId, user, content);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.readMessage(messageId, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/{messageId}/report")
    public ResponseEntity<ApiResponse<Void>> reportMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.reportMessage(messageId, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/block/{blockedId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long blockedId) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.blockUser(user, blockedId);
        return ApiResponse.success(null);
    }
}
