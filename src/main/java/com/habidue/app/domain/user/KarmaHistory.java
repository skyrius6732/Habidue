package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "karma_history")
public class KarmaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KarmaReason reason; // 변동 사유

    @Column(nullable = false)
    private int pointChange; // 변동 점수 (양수/음수)

    @Column(nullable = false)
    private int resultingPoint; // 변동 후 최종 점수

    private String comment; // 상세 사유 (예: "신고된 게시글 제목: [안녕...]")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin; // 조치를 취한 관리자 (시스템 자동일 경우 null)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
