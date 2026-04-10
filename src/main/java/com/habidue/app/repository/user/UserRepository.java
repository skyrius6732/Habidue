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
    Optional<User> findByPublicId(String publicId);
    List<User> findAllByPublicIdIn(java.util.Collection<String> publicIds);
    Optional<User> findByUsername(String username);
    
    // [시니어 조치] 재가입 방지(7일) 로직을 위해 탈퇴한 유저도 이메일/소셜ID로 조회 가능해야 함
    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // [시니어 조치] 가입 가능 여부 체크 시에는 '활성 유저' 중에서만 중복을 체크해야 함
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.status = 'ACTIVE'")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.nickname = :nickname AND u.status = 'ACTIVE'")
    boolean existsByNickname(@Param("nickname") String nickname);

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
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.totalExp = CASE WHEN (u.totalExp + :delta) < 0 THEN 0 ELSE (u.totalExp + :delta) END WHERE u.id = :id")
    void updateExp(@Param("id") Long id, @Param("delta") long delta);

    // [시니어 조치] Atomic 신뢰 점수 업데이트 (0~1000P 제한)
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.karmaPoint = CASE " +
           "WHEN (u.karmaPoint + :delta) < 0 THEN 0 " +
           "WHEN (u.karmaPoint + :delta) > 1000 THEN 1000 " +
           "ELSE (u.karmaPoint + :delta) END WHERE u.id = :id")
    void updateKarmaPoint(@Param("id") Long id, @Param("delta") int delta);

    // [시니어 조치] 레벨 업데이트
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.level = :level WHERE u.id = :id")
    void updateLevel(@Param("id") Long id, @Param("level") int level);

    List<User> findAllByStatus(com.habidue.app.domain.user.UserStatus status);
}
