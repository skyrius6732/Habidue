package com.habidue.app.controller.api;

import com.habidue.app.service.notification.NotificationScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발 환경에서 스케줄러 등을 강제 실행하기 위한 테스트 API
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final NotificationScheduler notificationScheduler;

    /**
     * 마감 임박 알림 스케줄러 강제 실행
     * 호출: GET /api/test/trigger-deadline-check
     */
    @GetMapping("/trigger-deadline-check")
    public String triggerDeadline() {
        System.out.println("========== [TEST API] Triggering Deadline Check Start ==========");
        log.info("[TEST API] trigger-deadline-check called.");
        
        try {
            notificationScheduler.sendDeadlineImminentNotifications();
            System.out.println("========== [TEST API] Triggering Deadline Check Success ==========");
            return "Deadline check triggered successfully! Check server console for detail logs.";
        } catch (Exception e) {
            System.err.println("========== [TEST API] Triggering Deadline Check FAILED ==========");
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }
}
