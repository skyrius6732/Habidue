package com.habidue.app.repository.user;

import com.habidue.app.domain.user.UserActivityStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityStatsRepository extends JpaRepository<UserActivityStats, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserActivityStats s WHERE s.userId = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE UserActivityStats s SET " +
            "s.totalPostCount = 0, s.totalCommentCount = 0, s.postLikeReceivedCount = 0, " +
            "s.commentLikeReceivedCount = 0, s.totalViewReceivedCount = 0, s.totalNoticeInterestCount = 0, " +
            "s.consecutiveAttendanceDays = 0, s.totalAttendanceCount = 0, s.reviewPostCount = 0, " +
            "s.lastAttendanceDate = null WHERE s.userId = :userId")
    void resetStatsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
