package com.habidue.app.domain.user;

import com.habidue.app.domain.tag.UserTag;
import com.habidue.app.domain.usernotice.UserNotice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_total_exp", columnList = "totalExp")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "providerId"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String publicId; // [시니어 조치] 외부 노출용 공개 ID (u_난수 형태)

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String nickname; // 커뮤니티용 닉네임

    @Column // 소셜 로그인 사용자는 패스워드가 없을 수 있음
    private String password;

    @Column(nullable = false)
    private String email;

    private String provider;   // 예: google
    private String providerId; // 구글의 고유 아이디 (sub)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailReportEnabled = true; // 메일 리포트 수신 여부

    private String reportEmail; // 리포트 수신용 이메일

    @Builder.Default
    @Column(nullable = false)
    private boolean reportEmailVerified = false; // 이메일 인증 여부

    // 연관 데이터 자동 삭제 설정 (Cascade)
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserNotice> userNotices = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTag> userTags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();

    // [시니어 조치] 활동 통계와의 양방향 관계 설정 (Cascade 적용으로 생성/삭제 동기화)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserActivityStats activityStats;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE; // 계정 상태 (기본값: ACTIVE)

    private String blockedReason; // 차단 사유

    private LocalDateTime blockedAt; // 차단 일시

    private LocalDateTime withdrawalAt; // 탈퇴 일시

    private LocalDateTime lastLoginAt; // [시니어 조치] 마지막 로그인 시점 기록

    // [시니어 조치] 활동 신뢰도 및 페널티 시스템 필드
    @Builder.Default
    @Column(nullable = false, columnDefinition = "int default 1000")
    private int karmaPoint = 1000; // 신뢰 점수 (기본 1000, 신고 승인 시 차감)

    private LocalDateTime restrictedUntil; // 활동 제한(글쓰기/댓글) 종료 시각

    @Builder.Default
    @Column(nullable = false)
    private int penaltyCount = 0; // 누적 페널티 횟수

    // [시니어 조치] Gamification System 필드
    @Builder.Default
    @Column(nullable = false)
    private long totalExp = 0L; // 누적 경험치

    @Builder.Default
    @Column(nullable = false)
    private int level = 1; // 현재 레벨

    private Long equippedBadgeId; // 장착 중인 대표 칭호(배지) ID

    @Builder.Default
    @Column(nullable = false)
    private boolean showLevelEffects = true; // 닉네임 특수 효과 표시 여부 (B 방법 적용)

    @Builder.Default
    @Column(nullable = false)
    private boolean showEquippedEffect = true; // [시니어 조치] 장착 중인 이펙트 효과(날개 등) 표시 여부

    private Integer equippedTier; // [시니어 조치] 장착 중인 닉네임 스타일 티어 (null이면 레벨 기준 자동)

    private String equippedEffect; // [시니어 조치] 장착 중인 특수 효과 코드 (날개, 오라 등)

    @Builder.Default
    @Column(nullable = false)
    private boolean betaTester = false; // [시니어 조치] 베타테스터 여부

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEffect> userEffects = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // [시니어 조치] 영속화 전 공개 ID 자동 생성 로직
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null) {
            this.publicId = "u_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
    }

    // [시니어 조치] 계정 탈퇴 및 익명화 로직
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalAt = LocalDateTime.now();
        
        // [시니어 조치] 재가입 방지(7일) 로직을 위해 식별 정보(username, email, providerId)를 보존함.
        // 탈퇴한 유저의 정보는 status가 WITHDRAWN이므로 로그인 시도는 차단됨.
        // 7일 이후 배치를 통해 이 필드들을 실제 익명화 처리하는 것이 가장 이상적임.
        
        this.password = null;
        this.totalExp = 0;
        this.level = 1;
        this.karmaPoint = 0;
        this.equippedBadgeId = null;
        // 기존 익명화 로직 제거 (재가입 방지 로직 작동을 위해 원본 유지)
    }
}
