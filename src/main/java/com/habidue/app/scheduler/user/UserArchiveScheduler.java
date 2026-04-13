package com.habidue.app.scheduler.user;

import com.habidue.app.repository.user.UserArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [시니어 조치] 탈퇴 유저 아카이브 자동 파기 스케줄러
 * - 개인정보보호법에 따라 수사 협조용 보존 기간(6개월)이 지난 데이터를 자동으로 영구 삭제함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserArchiveScheduler {

    private final UserArchiveRepository userArchiveRepository;

    /**
     * 매일 새벽 4시에 실행 (0 0 4 * * *)
     * 탈퇴 시점(withdrawnAt)으로부터 180일(약 6개월)이 지난 아카이브 데이터를 삭제함.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldArchives() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(180);
        
        log.info("[Scheduler] Starting cleanup of user archives older than {}", threshold);
        
        // withdrawnAt이 threshold 이전인 데이터 삭제
        int deletedCount = userArchiveRepository.deleteByWithdrawnAtBefore(threshold);
        
        if (deletedCount > 0) {
            log.info("[Scheduler] Successfully deleted {} expired user archives.", deletedCount);
        } else {
            log.info("[Scheduler] No expired user archives found to delete.");
        }
    }
}
