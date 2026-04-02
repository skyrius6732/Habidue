package com.habidue.app.repository.user;

import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status != 'WITHDRAWN'")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.status != 'WITHDRAWN'")
    Optional<User> findByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countRecent(@Param("startDate") LocalDateTime startDate);

    // 대시보드용 유저 상태별 건수
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> getCountByStatus();

    // 전체 랭킹 (누적 경험치 기준 상위 N명)
    org.springframework.data.domain.Page<User> findAllByOrderByTotalExpDesc(org.springframework.data.domain.Pageable pageable);

    // 내 순위 계산 (나보다 경험치가 높은 사람 수 + 1)
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.totalExp > :totalExp")
    long calculateRankByTotalExp(@Param("totalExp") long totalExp);

    // [시니어 조치] Atomic 경험치 업데이트
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.totalExp = CASE WHEN (u.totalExp + :delta) < 0 THEN 0 ELSE (u.totalExp + :delta) END WHERE u.id = :id")
    void updateExp(@Param("id") Long id, @Param("delta") long delta);

    // [시니어 조치] Atomic 신뢰 점수 업데이트 (0~1000P 제한)
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.karmaPoint = CASE " +
           "WHEN (u.karmaPoint + :delta) < 0 THEN 0 " +
           "WHEN (u.karmaPoint + :delta) > 1000 THEN 1000 " +
           "ELSE (u.karmaPoint + :delta) END WHERE u.id = :id")
    void updateKarmaPoint(@Param("id") Long id, @Param("delta") int delta);

    // [시니어 조치] 레벨 업데이트
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.level = :level WHERE u.id = :id")
    void updateLevel(@Param("id") Long id, @Param("level") int level);
}
