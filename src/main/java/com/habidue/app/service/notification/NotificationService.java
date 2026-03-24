package com.habidue.app.service.notification;

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
    
    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 60분으로 연장

    /**
     * SSE 구독
     */
    public SseEmitter subscribe(Long userId) {
        log.info("[SSE-DEBUG] === Subscribe Start: User {} ===", userId);
        
        // 기존 연결 정리
        if (emitters.containsKey(userId)) {
            log.info("[SSE-DEBUG] Found old emitter for user {}. Completing and removing.", userId);
            SseEmitter oldEmitter = emitters.get(userId);
            try { 
                oldEmitter.complete(); 
            } catch (Exception e) {
                log.warn("[SSE-DEBUG] Error completing old emitter: {}", e.getMessage());
            }
            emitters.remove(userId);
        }
        
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);
        
        emitter.onCompletion(() -> {
            log.info("[SSE-DEBUG] Connection Completed: User {}", userId);
            emitters.remove(userId, emitter); // [중요] 내가 등록한 '그' 객체일 때만 삭제
        });
        emitter.onTimeout(() -> { 
            log.info("[SSE-DEBUG] Connection Timeout: User {}", userId);
            emitter.complete(); 
            emitters.remove(userId, emitter); // [중요] 내가 등록한 '그' 객체일 때만 삭제
        });
        emitter.onError((e) -> { 
            log.error("[SSE-DEBUG] Connection Error: User {}: {}", userId, e.getMessage());
            emitter.complete(); 
            emitters.remove(userId, emitter); // [중요] 내가 등록한 '그' 객체일 때만 삭제
        });
        
        // 초기 연결 성공 알림 및 더미 데이터 전송 (연결 유지용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!")
                    .reconnectTime(3000)); // 끊겼을 때 3초 후 재연결 시도 지시
            log.info("[SSE-DEBUG] Initial 'connect' event sent to user: {}", userId);
        } catch (IOException e) { 
            log.error("[SSE-DEBUG] Failed to send initial event: {}", userId);
            emitters.remove(userId); 
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
                emitters.remove(userId); 
            }
        } else {
            log.warn("[SSE-DEBUG] !!! No Emitter in Map for User: {} !!! (This is why real-time fails)", userId);
            // 현재 활성화된 유저들 로그 출력 (디버깅용)
            log.info("[SSE-DEBUG] Currently Active Users in Map: {}", emitters.keySet());
        }
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
        int updatedCount = notificationRepository.forceReadAllByUserId(userId);
        log.info("[Notification] Bulk force read update: User {}, Count: {}", userId, updatedCount);
        List<Notification> stillUnread = notificationRepository.findAllUnreadByUserId(userId);
        if (!stillUnread.isEmpty()) {
            for (Notification n : stillUnread) { n.markAsRead(); }
            notificationRepository.saveAll(stillUnread);
        }
    }
}
