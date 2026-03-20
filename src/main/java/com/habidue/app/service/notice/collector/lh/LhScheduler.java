package com.habidue.app.service.notice.collector.lh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LhScheduler {

    private final LhNoticeCollectorService lhNoticeCollectorService;

    // 매일 새벽 2시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleLhNoticeCollection() {
        log.info("LH 공고 스케줄러 실행: 데이터 수집 시작");
        lhNoticeCollectorService.collectAndSaveLhNotices();
        log.info("LH 공고 스케줄러 실행: 데이터 수집 완료");
    }
}
