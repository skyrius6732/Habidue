package com.habidue.app.service.notice.collector.civil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateScheduler {

    private final PrivateNoticeCrawlerService privateNoticeCrawlerService;

    // 매일 새벽 2시 10분에 실행 (LH: 02:00, SH: 02:05 와 간격 유지)
    @Scheduled(cron = "0 10 2 * * *")
    public void schedulePrivateNoticeCollection() {
        log.info("민간임대(청년안심주택) 공고 스케줄러 실행: 데이터 수집 시작");
        privateNoticeCrawlerService.crawlPrivateNotices();
        log.info("민간임대(청년안심주택) 공고 스케줄러 실행: 데이터 수집 완료");
    }
}
