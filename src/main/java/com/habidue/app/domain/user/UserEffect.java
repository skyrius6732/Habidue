package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * [시니어 조치] 사용자 이펙트 획득 및 관리
 * - 레벨: 자동 오픈 (DB 기록 필요 없음, 로직으로만 확인)
 * - 베타/이벤트/상점: 획득 여부를 DB에서 관리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_effects", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_effect_code", columnNames = {"user_id", "effect_code"})
})
public class UserEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String effectCode; // 이펙트 코드 (예: PIONEER_WINGS, ICE_FROST)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffectSource source; // 획득 경로 (BETA, EVENT, SHOP, LEVEL)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime obtainedAt; // 획득 일시

    public enum EffectSource {
        BETA,      // 베타 테스트
        EVENT,     // 이벤트
        SHOP,      // 상점 구매
        LEVEL      // 레벨 업 (자동, 명시적 기록 없음)
    }
}
