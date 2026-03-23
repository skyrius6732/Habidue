package com.habidue.app.domain.message;

import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_blocks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_block", columnNames = {"blocker_id", "blocked_id"})
})
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker; // 차단한 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked; // 차단당한 사람

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // [시니어 조치] 차단 사유 및 시스템 자동 차단 여부 필드 추가
    @Column(length = 255)
    private String reason; // 차단 사유 (예: "심각한 운영원칙 위반으로 인한 자동 차단")

    @Builder.Default
    @Column(nullable = false)
    private boolean isSystemBlock = false; // 시스템에 의한 강제 차단 여부
}
