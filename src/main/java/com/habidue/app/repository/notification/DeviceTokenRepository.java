package com.habidue.app.repository.notification;

import com.habidue.app.domain.notification.DeviceToken;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    
    // 특정 유저의 모든 등록된 기기 토큰 조회
    List<DeviceToken> findByUser(User user);
    
    // 특정 유저의 ID로 모든 기기 토큰 조회
    List<DeviceToken> findByUserId(Long userId);
    
    // 이미 등록된 토큰인지 확인
    Optional<DeviceToken> findByToken(String token);
    
    // 특정 토큰 삭제 (로그아웃 시 등)
    void deleteByToken(String token);
}
