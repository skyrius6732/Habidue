package com.habidue.app.repository.notification;

import com.habidue.app.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 유저 ID의 알림 목록 최신순 조회
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 유저 ID의 읽지 않은 알림 수 카운트
    long countByUserIdAndIsReadFalse(Long userId);

    // 특정 유저 ID의 모든 알림 일괄 읽음 처리
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
