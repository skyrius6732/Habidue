package com.habidue.app.domain.badge;

import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 유저가 획득한 배지 연관관계 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_badges", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_id"})
})
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Builder.Default
    private int level = 1;

    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRepresentative = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime acquiredAt; // 획득 일시
    
    public void setRepresentative(boolean representative) {
        this.isRepresentative = representative;
    }

    public void updateLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
}
