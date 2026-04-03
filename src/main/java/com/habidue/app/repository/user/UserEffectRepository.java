package com.habidue.app.repository.user;

import com.habidue.app.domain.user.UserEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEffectRepository extends JpaRepository<UserEffect, Long> {

    /**
     * 사용자가 특정 이펙트를 소유하고 있는지 확인
     */
    boolean existsByUserIdAndEffectCode(Long userId, String effectCode);

    /**
     * 사용자의 모든 소유 이펙트 조회
     */
    List<UserEffect> findByUserId(Long userId);

    /**
     * 사용자가 소유한 특정 이펙트 조회
     */
    Optional<UserEffect> findByUserIdAndEffectCode(Long userId, String effectCode);

    /**
     * 사용자의 특정 소스의 이펙트 목록 조회
     */
    @Query("SELECT ue FROM UserEffect ue WHERE ue.user.id = :userId AND ue.source = :source")
    List<UserEffect> findByUserIdAndSource(@Param("userId") Long userId, @Param("source") UserEffect.EffectSource source);

    /**
     * 베타테스터 전용 이펙트 소유 확인 (PIONEER_WINGS)
     */
    @Query("SELECT CASE WHEN COUNT(ue) > 0 THEN true ELSE false END FROM UserEffect ue " +
            "WHERE ue.user.id = :userId AND ue.effectCode = :effectCode")
    boolean hasEffect(@Param("userId") Long userId, @Param("effectCode") String effectCode);
}
