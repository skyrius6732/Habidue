package com.habidue.app.service.notification;

import com.habidue.app.domain.notification.DeviceToken;
import com.habidue.app.repository.notification.DeviceTokenRepository;
import com.habidue.app.domain.notification.Notification;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.user.User;
import com.habidue.app.repository.notification.NotificationRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.dto.notification.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService { // [시니어 조치] 클래스 레벨 @Transactional 제거

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FCMService fcmService;
    
    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 1000L * 60 * 60; // 60분

    /**
     * FCM 토큰 저장 (기기 등록)
     */
    @Transactional
    public void saveFcmToken(User user, String token) {
        log.info("[FCM] Saving token for user: {}, token: {}", user.getId(), token);
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
            existingToken -> {
                existingToken.setUser(user); // 혹시 유저가 바뀌었을 경우 업데이트
                deviceTokenRepository.save(existingToken);
            },
            () -> {
                deviceTokenRepository.save(DeviceToken.builder()
                        .user(user)
                        .token(token)
                        .build());
            }
        );
    }

    /**
     * [시니어 조치] 연결 유지용 하트비트 스케줄러 (더 짧은 주기로 변경)
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 25000) // 25초마다 실행
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((userId, emitter) -> {
            try {
                // [시니어 조치] 가장 안전한 형태의 핑 (이벤트명 없이 데이터만)
                emitter.send(SseEmitter.event().data("ping"));
            } catch (Exception e) {
                emitters.remove(userId, emitter);
            }
        });
    }

    /**
     * SSE 구독
     */
    public SseEmitter subscribe(Long userId) {
        log.info("[SSE-DEBUG] === Subscribe Start: User {} ===", userId);
        
        SseEmitter oldEmitter = emitters.get(userId);
        if (oldEmitter != null) {
            log.info("[SSE-DEBUG] Found old emitter for user {} (Hash: {}). Completing.", userId, oldEmitter.hashCode());
            try { oldEmitter.complete(); } catch (Exception e) {}
            emitters.remove(userId, oldEmitter);
        }
        
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        int emitterHash = emitter.hashCode();
        
        emitter.onCompletion(() -> {
            log.info("[SSE-DEBUG] Connection Completed (OnCompletion): User {} (Hash: {})", userId, emitterHash);
            emitters.remove(userId, emitter);
        });
        emitter.onTimeout(() -> { 
            log.info("[SSE-DEBUG] Connection Timeout (OnTimeout): User {} (Hash: {})", userId, emitterHash);
            emitter.complete(); 
            emitters.remove(userId, emitter);
        });
        emitter.onError((e) -> { 
            log.error("[SSE-DEBUG] Connection Error (OnError): User {} (Hash: {}): {}", userId, emitterHash, e.getMessage());
            emitter.complete(); 
            emitters.remove(userId, emitter);
        });
        
        emitters.put(userId, emitter);
        log.info("[SSE-DEBUG] New Emitter Registered: User {} (Hash: {})", userId, emitterHash);
        
        try {
            // [시니어 조치] 브라우저의 자동 재시도를 늦추기 위해 reconnectTime을 매우 길게 설정 (1시간)
            // 실제 재연결은 프론트엔드에서 새 토큰으로 제어함
            emitter.send(SseEmitter.event()
                    .data("connected")
                    .reconnectTime(3600000)); 
            log.info("[SSE-DEBUG] Registered & Connected: User {} (Hash: {})", userId, emitterHash);
        } catch (Exception e) { 
            emitters.remove(userId, emitter); 
        }
        
        return emitter;
    }

    @Transactional
    public void send(User receiver, NotificationType type, String content, Long relatedTargetId, Long postId) {
        log.info("[SSE-DEBUG] >>> Sending Notification: User {}, Type: {}", receiver.getId(), type);
        Notification notification = Notification.builder()
                .user(receiver)
                .type(type)
                .content(content)
                .relatedTargetId(relatedTargetId)
                .postId(postId)
                .readStatus(0)
                .build();
        Notification savedNoti = notificationRepository.saveAndFlush(notification);
        Long userId = receiver.getId();
        
        // [FCM 조치] 모바일 푸시 알림 발송 경로 생성
        String clickAction = "/";
        if (type == NotificationType.LIKE || type == NotificationType.COMMENT || type == NotificationType.REPLY) {
            if (postId != null) {
                clickAction = "/board/post/" + postId;
                // 댓글/답글의 경우 상세 위치로 이동하기 위해 쿼리 파라미터 추가
                if (relatedTargetId != null && (type == NotificationType.COMMENT || type == NotificationType.REPLY)) {
                    clickAction += "?commentId=" + relatedTargetId;
                }
            }
        } else if (type == NotificationType.MESSAGE) {
            clickAction = "/keywords?tab=messages";
        } else {
            clickAction = "/keywords?tab=notifications";
        }

        fcmService.sendPushNotification(userId, "habiDue 알림", content, clickAction);
        
        // 트랜잭션 커밋 후에 실제로 SSE 발송
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { 
                    log.info("[SSE-DEBUG] Transaction Commit Done. Now pushing SSE to user: {}", userId);
                    sendSseNotification(userId, savedNoti); 
                }
            });
        } else { 
            sendSseNotification(userId, savedNoti); 
        }
    }

    private void sendSseNotification(Long userId, Notification notification) {
        if (emitters.containsKey(userId)) {
            SseEmitter emitter = emitters.get(userId);
            try {
                log.info("[SSE-DEBUG] Pushing Data... User: {}, NotiID: {}", userId, notification.getId());
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name("message") // 이벤트 이름 명시
                        .data(NotificationResponseDto.from(notification)));
                log.info("[SSE-DEBUG] Pushing Success! User: {}", userId);
            } catch (Exception e) { 
                log.error("[SSE-DEBUG] Pushing Failed. User: {}, Reason: {}", userId, e.getMessage());
                emitters.remove(userId, emitter); 
            }
        } else {
            log.warn("[SSE-DEBUG] !!! No Emitter in Map for User: {} !!! (This is why real-time fails)", userId);
            // 현재 활성화된 유저들 로그 출력 (디버깅용)
            log.info("[SSE-DEBUG] Currently Active Users in Map: {}", emitters.keySet());
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUserId(Long userId, org.springframework.data.domain.Pageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCountByUserId(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("알림을 찾을 수 없습니다."));
        if (notification.getReadStatus() == 0) {
            notification.markAsRead();
            notificationRepository.saveAndFlush(notification);
        }
    }

    @Transactional
    public void markAllAsReadByUserId(Long userId) {
        // [시니어 조치] 최적화된 벌크 쿼리 한 번으로 모든 처리 종료 (네트워크 타임아웃 방지)
        int updatedCount = notificationRepository.forceReadAllByUserId(userId);
        log.info("[Notification] Bulk read update completed: User {}, Count: {}", userId, updatedCount);
    }

    /**
     * [시니어 조치] 전 유저 대상 실시간 알림 발송 (비동기 처리)
     */
    @org.springframework.scheduling.annotation.Async
    @Transactional
    public void sendToAllUsers(NotificationType type, String content, Long relatedTargetId, Long postId) {
        log.info("[SSE-DEBUG] >>> Broadcasting Notification: Type: {}", type);
        List<User> allUsers = userRepository.findAll();
        
        // 공통 클릭 경로 생성
        String clickAction = "/keywords?tab=notifications";
        if (postId != null) {
            clickAction = "/board/post/" + postId;
        }

        for (User receiver : allUsers) {
            Notification notification = Notification.builder()
                    .user(receiver)
                    .type(type)
                    .content(content)
                    .relatedTargetId(relatedTargetId)
                    .postId(postId)
                    .readStatus(0)
                    .build();
            notificationRepository.save(notification);
            
            // [FCM 조치] 모바일 푸시 알림 발송 (경로 포함)
            fcmService.sendPushNotification(receiver.getId(), "habiDue 공지", content, clickAction);
            
            // SSE 실시간 전송 시도
            sendSseNotification(receiver.getId(), notification);
        }
        log.info("[SSE-DEBUG] Broadcasting completed for {} users.", allUsers.size());
    }
}
