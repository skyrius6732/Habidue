package com.habidue.app.repository.user;

import com.habidue.app.domain.user.UserActivityStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityStatsRepository extends JpaRepository<UserActivityStats, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserActivityStats s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET " +
            "s.totalPostCount = 0, s.totalCommentCount = 0, s.postLikeReceivedCount = 0, " +
            "s.commentLikeReceivedCount = 0, s.totalViewReceivedCount = 0, s.totalNoticeInterestCount = 0, " +
            "s.consecutiveAttendanceDays = 0, s.totalAttendanceCount = 0, s.reviewPostCount = 0, " +
            "s.lastAttendanceDate = null WHERE s.userId = :userId")
    void resetStatsByUserId(@Param("userId") Long userId);

    // --- 원자적 카운터 업데이트 (race condition 방지) ---

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalPostCount = s.totalPostCount + 1 WHERE s.userId = :userId")
    void incrementPostCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalPostCount = s.totalPostCount - 1 WHERE s.userId = :userId AND s.totalPostCount > 0")
    void decrementPostCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.reviewPostCount = s.reviewPostCount + 1 WHERE s.userId = :userId")
    void incrementReviewPostCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.reviewPostCount = s.reviewPostCount - 1 WHERE s.userId = :userId AND s.reviewPostCount > 0")
    void decrementReviewPostCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalCommentCount = s.totalCommentCount + 1 WHERE s.userId = :userId")
    void incrementCommentCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalCommentCount = s.totalCommentCount - 1 WHERE s.userId = :userId AND s.totalCommentCount > 0")
    void decrementCommentCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.postLikeReceivedCount = s.postLikeReceivedCount + 1 WHERE s.userId = :userId")
    void incrementPostLikeReceivedCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.postLikeReceivedCount = s.postLikeReceivedCount - 1 WHERE s.userId = :userId AND s.postLikeReceivedCount > 0")
    void decrementPostLikeReceivedCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE user_activity_stats SET post_like_received_count = GREATEST(0, post_like_received_count - :count) WHERE user_id = :userId", nativeQuery = true)
    void decrementPostLikeReceivedCountBy(@Param("userId") Long userId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.commentLikeReceivedCount = s.commentLikeReceivedCount + 1 WHERE s.userId = :userId")
    void incrementCommentLikeReceivedCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.commentLikeReceivedCount = s.commentLikeReceivedCount - 1 WHERE s.userId = :userId AND s.commentLikeReceivedCount > 0")
    void decrementCommentLikeReceivedCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE user_activity_stats SET comment_like_received_count = GREATEST(0, comment_like_received_count - :count) WHERE user_id = :userId", nativeQuery = true)
    void decrementCommentLikeReceivedCountBy(@Param("userId") Long userId, @Param("count") int count);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalViewReceivedCount = s.totalViewReceivedCount + 1 WHERE s.userId = :userId")
    void incrementViewReceivedCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalNoticeInterestCount = s.totalNoticeInterestCount + 1 WHERE s.userId = :userId")
    void incrementNoticeInterestCount(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserActivityStats s SET s.totalNoticeInterestCount = s.totalNoticeInterestCount - 1 WHERE s.userId = :userId AND s.totalNoticeInterestCount > 0")
    void decrementNoticeInterestCount(@Param("userId") Long userId);

    // [시니어 조치] 원자적 생성 (이미 존재하면 무시)
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT IGNORE INTO user_activity_stats (user_id, total_post_count, total_comment_count, post_like_received_count, comment_like_received_count, total_view_received_count, total_notice_interest_count, consecutive_attendance_days, total_attendance_count, review_post_count, updated_at) " +
                   "VALUES (:userId, 0, 0, 0, 0, 0, 0, 0, 0, 0, NOW())", nativeQuery = true)
    void insertIgnore(@Param("userId") Long userId);
}
