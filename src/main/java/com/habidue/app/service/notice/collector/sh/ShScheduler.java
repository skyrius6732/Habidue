package com.habidue.app.service.notice.collector.sh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShScheduler {

    private final ShNoticeCrawlerService shNoticeCrawlerService;

    // 매일 새벽 2시 5분에 실행 (초 분 시 일 월 요일)
    // LH 스케줄러(02:00)와 5분 간격을 둠
    @Scheduled(cron = "0 5 2 * * *")
    public void scheduleShNoticeCollection() {
        log.info("SH 공고 스케줄러 실행: 데이터 수집 시작");
        shNoticeCrawlerService.crawlShNotices();
        log.info("SH 공고 스케줄러 실행: 데이터 수집 완료");
    }
}
