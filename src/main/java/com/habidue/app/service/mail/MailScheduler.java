package com.habidue.app.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailScheduler {

    private final ReportService reportService;

    // 매주 월요일 오전 9시에 실행 (cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReport() {
        log.info("주간 메일 리포트 발송 시작...");
        reportService.generateAndSendWeeklyReports();
        log.info("주간 메일 리포트 발송 완료.");
    }
}
