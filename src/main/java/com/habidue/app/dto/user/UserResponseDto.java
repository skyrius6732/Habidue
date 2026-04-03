package com.habidue.app.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.user.User;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String status; // ACTIVE, BLOCKED, WITHDRAWN
    private String blockedReason;
    private LocalDateTime blockedAt;
    private boolean emailReportEnabled;
    private String reportEmail;
    private boolean reportEmailVerified;
    private LocalDateTime createdAt;
    
    // [시니어 조치] 레벨링 시스템 정보
    private int level;
    private long totalExp;
    private Long equippedBadgeId;
    private boolean showLevelEffects;
    private boolean showEquippedEffect; // [시니어 조치] 이펙트 효과 표시 여부 추가
    private Integer equippedTier; // [시니어 조치] 장착 중인 티어 스타일
    private String equippedEffect; // [시니어 조치] 장착 중인 특수 효과 코드 추가
    
    @JsonProperty("isOnline")
    private boolean isOnline; // 실시간 접속 여부 추가

    // [시니어 조치] 활동 신뢰 및 페널티 정보 추가
    private int karmaPoint;
    private LocalDateTime restrictedUntil;

    public UserResponseDto(User user) {
        this(user, false);
    }

    public UserResponseDto(User user, boolean isOnline) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.status = user.getStatus() != null ? user.getStatus().name() : "ACTIVE";
        this.blockedReason = user.getBlockedReason();
        this.blockedAt = user.getBlockedAt();
        this.emailReportEnabled = user.isEmailReportEnabled();
        this.reportEmail = user.getReportEmail();
        this.reportEmailVerified = user.isReportEmailVerified();
        this.createdAt = user.getCreatedAt();
        
        // 레벨 시스템 매핑
        this.level = user.getLevel();
        this.totalExp = user.getTotalExp();
        this.equippedBadgeId = user.getEquippedBadgeId();
        this.showLevelEffects = user.isShowLevelEffects();
        this.showEquippedEffect = user.isShowEquippedEffect(); // [시니어 조치] 이펙트 표시 여부 매핑
        this.equippedTier = user.getEquippedTier();
        this.equippedEffect = user.getEquippedEffect();

        // [시니어] 카르마 시스템 매핑
        this.karmaPoint = user.getKarmaPoint();
        this.restrictedUntil = user.getRestrictedUntil();

        this.isOnline = isOnline;
    }
}
