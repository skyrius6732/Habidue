package com.habidue.app.domain.notification;

import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String content;

    private Long relatedTargetId; // 댓글ID, 쪽지상대ID 등
    
    // [시니어 조치] 게시글 알림 시 바로 상세 페이지로 가기 위한 포스트 ID 저장
    private Long postId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private int readStatus = 0; // 0: 안읽음, 1: 읽음

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markAsRead() {
        this.readStatus = 1;
    }

    public boolean isRead() {
        return this.readStatus == 1;
    }
}
