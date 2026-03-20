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
     * 특정 사용자가 특정 기간(오늘 하루) 동안 획득(양수)한 카르마 점수의 총합을 구함
     */
    @Query("SELECT COALESCE(SUM(kh.pointChange), 0) FROM KarmaHistory kh " +
           "WHERE kh.user.id = :userId AND kh.pointChange > 0 AND kh.createdAt >= :since")
    int getDailyEarnedPoints(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 특정 소스(게시글 등)로부터 획득한 카르마 점수의 총합을 구함
     */
    @Query("SELECT COALESCE(SUM(kh.pointChange), 0) FROM KarmaHistory kh " +
           "WHERE kh.user.id = :userId AND kh.reason = :reason AND kh.comment LIKE :postKey")
    int getPointsByPost(@Param("userId") Long userId, @Param("reason") KarmaReason reason, @Param("postKey") String postKey);
}
