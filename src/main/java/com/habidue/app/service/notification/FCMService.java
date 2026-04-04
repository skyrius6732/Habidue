package com.habidue.app.service.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.habidue.app.domain.notification.DeviceToken;
import com.habidue.app.repository.notification.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * 특정 유저의 모든 기기에 푸시 알림 전송 (이동 경로 포함)
     */
    public void sendPushNotification(Long userId, String title, String body, String clickAction) {
        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);
        
        if (deviceTokens.isEmpty()) {
            log.info("[FCM] No device tokens found for user: {}", userId);
            return;
        }

        // clickAction이 없으면 기본값으로 설정
        String action = (clickAction == null || clickAction.isEmpty()) ? "/" : clickAction;

        for (DeviceToken deviceToken : deviceTokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        // [시니어 조치] notification 객체를 제거하여 브라우저 자동 알림 중복 방지
                        // 대신 data 객체에 정보를 담아 서비스 워커(SW)가 단독으로 알림을 띄우도록 함
                        .putData("title", title)
                        .putData("body", body)
                        .putData("click_action", action) 
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("[FCM] Successfully sent message to token: {}. Response: {}", deviceToken.getToken(), response);
            } catch (Exception e) {
                log.error("[FCM] Failed to send push for user: {}, Token: {}. Error: {}", 
                        userId, deviceToken.getToken(), e.getMessage());
                
                // 만료된 토큰인 경우 DB에서 삭제 (cleanup)
                if (e.getMessage().contains("UNREGISTERED") || e.getMessage().contains("not-registered")) {
                    log.info("[FCM] Cleaning up unregistered token: {}", deviceToken.getToken());
                    deviceTokenRepository.delete(deviceToken);
                }
            }
        }
    }

    /**
     * 기존 하위 호환을 위한 메서드 (기본 경로 이동)
     */
    public void sendPushNotification(Long userId, String title, String body) {
        sendPushNotification(userId, title, body, "/");
    }
}
