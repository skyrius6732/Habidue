package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeWakeUp;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NoticeWakeUpRepository extends JpaRepository<NoticeWakeUp, Long> {
    Optional<NoticeWakeUp> findByNoticeAndUser(Notice notice, User user);
    
    /**
     * 특정 공고에 대해 특정 시점 이후의 깨우기 클릭 수 조회
     */
    long countByNoticeIdAndCreatedAtAfter(Long noticeId, LocalDateTime since);
    
    boolean existsByNoticeAndUser(Notice notice, User user);

    /**
     * [시니어 조치] 깨우기 성공 시 해당 공고의 모든 기록을 초기화
     */
    void deleteAllByNoticeId(Long noticeId);

    // 사용자 탈퇴 시 모든 공고 깨우기 기록 삭제
    void deleteByUserId(Long userId);
}
