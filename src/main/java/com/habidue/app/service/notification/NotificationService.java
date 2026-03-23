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
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 30L * 1000 * 60;

    public SseEmitter subscribe(Long userId) {
        if (emitters.containsKey(userId)) {
            SseEmitter oldEmitter = emitters.get(userId);
            try { oldEmitter.complete(); } catch (Exception e) {}
            emitters.remove(userId);
        }
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> { emitter.complete(); emitters.remove(userId); });
        emitter.onError((e) -> { emitter.complete(); emitters.remove(userId); });
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) { emitters.remove(userId); }
        return emitter;
    }

    /**
     * [시니어 조치] postId 파라미터 추가
     */
    public void send(User receiver, NotificationType type, String content, Long relatedTargetId, Long postId) {
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
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { sendSseNotification(userId, savedNoti); }
            });
        } else { sendSseNotification(userId, savedNoti); }
    }

    /**
     * 하위 호환을 위한 오버로딩 (기존 호출부 대응)
     */
    public void send(User receiver, NotificationType type, String content, Long relatedTargetId) {
        send(receiver, type, content, relatedTargetId, null);
    }

    private void sendSseNotification(Long userId, Notification notification) {
        if (emitters.containsKey(userId)) {
            SseEmitter emitter = emitters.get(userId);
            try {
                emitter.send(SseEmitter.event().id(String.valueOf(notification.getId())).data(NotificationResponseDto.from(notification)));
            } catch (Exception e) { emitters.remove(userId); }
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
            notificationRepository.saveAllAndFlush(stillUnread);
        }
    }
}
