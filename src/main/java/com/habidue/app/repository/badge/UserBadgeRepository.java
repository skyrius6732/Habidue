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
}
