package com.habidue.app.controller.api;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.notification.NotificationResponseDto;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

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
     * [시니어 조치] 로그 폭주 방지를 위해 WARN 로그 제거 및 401 반환 방식 최적화
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@RequestParam(value = "token", required = false) String token) {
        if (token != null && !token.isEmpty() && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserId(token);
            if (userId != null) {
                return ResponseEntity.ok()
                        .header("Cache-Control", "no-cache")
                        .header("X-Accel-Buffering", "no")
                        .body(notificationService.subscribe(userId));
            }
        }
        
        // 인증 실패 시 로그를 남기지 않고 401을 즉시 반환하여 브라우저 재시도를 제어
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * 내 알림 목록 조회 (페이징 지원)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (userPrincipal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<NotificationResponseDto> list = notificationService.getNotificationsByUserId(userPrincipal.getId(), pageable)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());

        return ApiResponse.success(list);
    }
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
        if (userPrincipal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // [시니어 조치] NullPointerException 방지를 위해 ID 전달 방식으로 복구
        notificationService.markAllAsReadByUserId(userPrincipal.getId());
        return ApiResponse.success(null);
    }
}
