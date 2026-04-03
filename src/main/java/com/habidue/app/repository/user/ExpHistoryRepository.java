package com.habidue.app.repository.user;

import com.habidue.app.domain.user.ExpHistory;
import com.habidue.app.domain.user.ExpReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpHistoryRepository extends JpaRepository<ExpHistory, Long> {

    List<ExpHistory> findByUserId(Long userId);

    // 특정 사용자의 기간 내 획득 경험치 총합
    @Query("SELECT COALESCE(SUM(e.acquiredExp), 0) FROM ExpHistory e WHERE e.user.id = :userId AND e.createdAt >= :startDate")
    long sumExpByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // 랭킹 집계용 Projection 인터페이스
    interface RankerProjection {
        Long getUserId();
        String getNickname();
        Integer getLevel();
        Long getTotalExp();
        Integer getKarmaPoint();
        Long getEquippedBadgeId();
        String getEquippedEffect(); // [시니어] 특수 효과
        boolean isShowLevelEffects(); // [시니어] 표시 여부
        boolean isShowEquippedEffect(); // [시니어 조치] 이펙트 표시 여부
    }

    @Query("SELECT e.user.id AS userId, e.user.nickname AS nickname, e.user.level AS level, SUM(e.acquiredExp) AS totalExp, e.user.karmaPoint AS karmaPoint, u.equippedBadgeId AS equippedBadgeId, u.equippedEffect AS equippedEffect, u.showLevelEffects AS showLevelEffects, u.showEquippedEffect AS showEquippedEffect " +
           "FROM ExpHistory e JOIN e.user u " +
           "WHERE e.createdAt >= :startDate AND e.reason IN :reasons " +
           "GROUP BY e.user.id, e.user.nickname, e.user.level, e.user.karmaPoint, u.equippedBadgeId, u.equippedEffect, u.showLevelEffects, u.showEquippedEffect " +
           "ORDER BY SUM(e.acquiredExp) DESC")
    org.springframework.data.domain.Page<RankerProjection> findTopRankersByPeriodAndReasons(@Param("startDate") LocalDateTime startDate, @Param("reasons") List<ExpReason> reasons, org.springframework.data.domain.Pageable pageable);

    // [시니어 조치] 이전 주기의 순위 집계를 위한 ID 리스트 조회
    @Query("SELECT e.user.id " +
           "FROM ExpHistory e " +
           "WHERE e.createdAt >= :startDate AND e.createdAt < :endDate AND e.reason IN :reasons " +
           "GROUP BY e.user.id " +
           "ORDER BY SUM(e.acquiredExp) DESC")
    List<Long> findTopRankerIdsByPeriodRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("reasons") List<ExpReason> reasons, org.springframework.data.domain.Pageable pageable);
}