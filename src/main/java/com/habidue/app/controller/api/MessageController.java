package com.habidue.app.controller.api;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * 쪽지 발송 (파일 첨부 지원)
     */
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("receiverId") String receiverId,
            @RequestPart("content") String content,
            @RequestPart(value = "files", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> files) {
        
        User sender = userService.getUserByUsername(userDetails.getUsername());
        messageService.sendMessage(sender.getId(), Long.valueOf(receiverId), content, files);
        return ApiResponse.success(null);
    }

    /**
     * 수신 쪽지함 조회
     */
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<Page<com.habidue.app.dto.message.MessageResponseDto>>> getReceivedMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        
        User user = userService.getUserByUsername(userDetails.getUsername());
        Page<Message> messages = messageService.getReceivedMessages(user, pageable);
        return ApiResponse.success(messages.map(com.habidue.app.dto.message.MessageResponseDto::from));
    }

    /**
     * 쪽지 읽음 처리
     */
    @PatchMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> readMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.readMessage(messageId, user);
        return ApiResponse.success(null);
    }

    /**
     * 쪽지 신고
     */
    @PostMapping("/{messageId}/report")
    public ResponseEntity<ApiResponse<Void>> reportMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.reportMessage(messageId, user);
        return ApiResponse.success(null);
    }

    /**
     * 유저 차단
     */
    @PostMapping("/block/{blockedId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long blockedId) {
        
        User user = userService.getUserByUsername(userDetails.getUsername());
        messageService.blockUser(user, blockedId);
        return ApiResponse.success(null);
    }
}
