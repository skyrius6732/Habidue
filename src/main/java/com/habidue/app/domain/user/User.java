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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // [시니어 조치] 계정 탈퇴 및 익명화 로직
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.email = "withdrawn_" + this.id + "@habidue.com"; // 유니크 제약 조건 회피용 가짜 이메일
        this.username = "withdrawn_" + this.id;
        this.nickname = "(탈퇴한 사용자)";
        this.providerId = null; // 재가입 가능하도록 함
        this.password = null;
        this.totalExp = 0;
        this.level = 1;
        this.karmaPoint = 0;
        this.equippedBadgeId = null;
    }
}
