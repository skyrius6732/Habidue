package com.habidue.app.repository.user;

import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countRecent(@Param("startDate") LocalDateTime startDate);

    // 전체 랭킹 (누적 경험치 기준 상위 N명)
    org.springframework.data.domain.Page<User> findAllByOrderByTotalExpDesc(org.springframework.data.domain.Pageable pageable);

    // 내 순위 계산 (나보다 경험치가 높은 사람 수 + 1)
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.totalExp > :totalExp")
    long calculateRankByTotalExp(@Param("totalExp") long totalExp);
}
