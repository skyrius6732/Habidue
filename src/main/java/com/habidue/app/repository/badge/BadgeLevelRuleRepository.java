package com.habidue.app.repository.badge;

import com.habidue.app.domain.badge.BadgeLevelRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeLevelRuleRepository extends JpaRepository<BadgeLevelRule, Long> {
    List<BadgeLevelRule> findAllByOrderByLevelAsc();
    List<BadgeLevelRule> findByBadgeTypeOrderByLevelAsc(String badgeType);
    
    // 특정 타입의 현재 수치에 도달한 가장 높은 레벨의 룰을 찾음
    BadgeLevelRule findTopByBadgeTypeAndRequiredValueLessThanEqualOrderByLevelDesc(String badgeType, int currentValue);

    // 특정 타입과 레벨에 정확히 일치하는 룰을 찾음 (실시간 명칭 조합용)
    BadgeLevelRule findByBadgeTypeAndLevel(String badgeType, int level);

    // [시니어] 현재 레벨보다 높은 가장 가까운 레벨의 룰을 찾음 (진행도 계산용)
    BadgeLevelRule findTopByBadgeTypeAndLevelGreaterThanOrderByLevelAsc(String badgeType, int level);

    /**
     * 특정 배지 타입의 모든 규칙을 삭제함
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByBadgeType(String badgeType);
}
