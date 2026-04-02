package com.habidue.app.repository.badge;

import com.habidue.app.domain.badge.Badge;
import com.habidue.app.domain.badge.UserBadge;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserOrderByAcquiredAtDesc(User user);
    Optional<UserBadge> findByUserAndBadge(User user, Badge badge); // [시니어 조치] 단건 조회 메서드 추가
    boolean existsByUserAndBadge(User user, Badge badge);
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId); // [시니어 조치] ID 기반 존재 여부 확인
    
    // [시니어] 유저 ID와 배지 ID로 단건 조회
    Optional<UserBadge> findByUserIdAndBadgeId(Long userId, Long badgeId);

    Optional<UserBadge> findByUserAndIsRepresentativeTrue(User user);

    @org.springframework.data.jpa.repository.Query("SELECT ub FROM UserBadge ub WHERE ub.user.id IN :userIds")
    List<UserBadge> findAllByUserIds(@Param("userIds") List<Long> userIds);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserBadge ub WHERE ub.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 현재 레벨보다 높을 때만 업데이트 (중복 승급 및 EXP 중복 지급 방지)
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE UserBadge ub SET ub.level = :newLevel WHERE ub.id = :id AND ub.level < :newLevel")
    int updateLevelIfLower(@Param("id") Long id, @Param("newLevel") int newLevel);
}
