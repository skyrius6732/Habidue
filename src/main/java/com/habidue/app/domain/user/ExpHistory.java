package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "exp_history", indexes = {
    @Index(name = "idx_exphistory_created_at_reason", columnList = "createdAt, reason")
})
public class ExpHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int acquiredExp; // 획득한 경험치 양

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100) // [시니어] 더 긴 Enum 이름을 수용할 수 있도록 100자로 확장
    private ExpReason reason; // 경험치 획득 사유

    private String description; // 부가 설명 (예: "특정 공고 소통방 첫 글 작성")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public static ExpHistory create(User user, int exp, ExpReason reason, String description) {
        return ExpHistory.builder()
                .user(user)
                .acquiredExp(exp)
                .reason(reason)
                .description(description)
                .build();
    }
}