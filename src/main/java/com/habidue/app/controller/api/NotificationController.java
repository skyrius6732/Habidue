package com.habidue.app.controller.api;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.notification.NotificationResponseDto;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider tokenProvider;

    /**
     * 알림 구독 (SSE)
     * [시니어 조치] EventSource의 헤더 미지원 문제를 해결하기 위해 쿼리 파라미터로 토큰을 받음
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("token") String token) {
        if (token != null && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserId(token);
            log.info("[SSE] User {} subscribed with token", userId);
            return notificationService.subscribe(userId);
        }
        throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
    }

    /**
     * 내 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(notificationService.getNotificationsByUserId(userPrincipal.getId())
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(notificationService.getUnreadCountByUserId(userPrincipal.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsReadByUserId(userPrincipal.getId());
        return ApiResponse.success(null);
    }
}
