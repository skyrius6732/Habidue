package com.habidue.app.repository.user;

import com.habidue.app.domain.user.KarmaHistory;
import com.habidue.app.domain.user.KarmaReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface KarmaHistoryRepository extends JpaRepository<KarmaHistory, Long> {
    List<KarmaHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * [시니어 조치] 사용자 활동(좋아요 수신 및 취소)으로 인한 순수 획득 점수 합산
     */
    @Query("SELECT COALESCE(SUM(kh.pointChange), 0) FROM KarmaHistory kh " +
           "WHERE kh.user.id = :userId AND (kh.reason = 'LIKE_RECEIVED' OR kh.reason = 'LIKE_CANCELED') AND kh.createdAt >= :since")
    int getDailyEarnedPoints(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 특정 소스(게시글 등)로부터 획득한 카르마 점수의 총합을 구함
     */
    @Query("SELECT COALESCE(SUM(kh.pointChange), 0) FROM KarmaHistory kh " +
           "WHERE kh.user.id = :userId AND kh.reason = :reason AND kh.comment = :postKey")
    int getPointsByPost(@Param("userId") Long userId, @Param("reason") KarmaReason reason, @Param("postKey") String postKey);
}
