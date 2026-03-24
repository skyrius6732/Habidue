package com.habidue.app.service.notification;

import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.repository.notice.UserNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 정기적인 알림 발송을 담당하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final UserNoticeRepository userNoticeRepository;
    private final NotificationService notificationService;

    /**
     * 매일 오전 9시에 마감 임박(D-1) 공고 알림을 발송함
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDeadlineImminentNotifications() {
        // 내일의 시작과 끝 시간 계산
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime tomorrowEnd = LocalDate.now().plusDays(1).atTime(LocalTime.MAX);

        log.info("[Notification] Checking deadlines between {} and {}", tomorrowStart, tomorrowEnd);

        List<UserNotice> imminentNotices = userNoticeRepository.findDeadlineImminentNotices(tomorrowStart, tomorrowEnd);

        log.info("[Notification] Query executed. Found {} user notices with deadline tomorrow.", imminentNotices.size());

        for (UserNotice un : imminentNotices) {
            log.info("[Notification] Sending deadline reminder to User: {}, Notice: {}", un.getUser().getId(), un.getNotice().getId());
            String content = String.format("⏰ 내일 마감되는 관심 공고가 있습니다: %s", un.getNotice().getTitle());
            notificationService.send(un.getUser(), NotificationType.NOTICE_DEADLINE, content, un.getNotice().getId(), null);
        }

        log.info("[Notification] Finished sending {} deadline imminent notifications.", imminentNotices.size());
    }
}
