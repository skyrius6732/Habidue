package com.habidue.app.repository.notification;

import com.habidue.app.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // [시니어 조치] readStatus 기반 쿼리
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.readStatus = 0")
    List<Notification> findAllUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readStatus = 0")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * [시니어 조치] 무조건적인 벌크 업데이트
     * 안 읽은 것만 골라서 하다가 매핑 문제로 누락되는 것을 방지하기 위해
     * 해당 유저의 모든 알림을 그냥 1(읽음)로 업데이트함.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readStatus = 1 WHERE n.user.id = :userId")
    int forceReadAllByUserId(@Param("userId") Long userId);
}
