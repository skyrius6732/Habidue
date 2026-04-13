package com.habidue.app.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.user.User;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 전용 사용자 응답 DTO (ID 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponseDto {
    private Long id; // 관리자용이므로 ID 노출
    private String publicId;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String status;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private boolean emailReportEnabled;
    private String reportEmail;
    private boolean reportEmailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    
    private int level;
    private long totalExp;
    private Long equippedBadgeId;
    private boolean showLevelEffects;
    private boolean showEquippedEffect;
    private Integer equippedTier;
    private String equippedEffect;
    private boolean betaTester;
    private List<String> ownedEffectCodes;

    @JsonProperty("isOnline")
    private boolean isOnline;

    private int karmaPoint;
    private LocalDateTime restrictedUntil;

    public static AdminUserResponseDto from(User user, boolean isOnline) {
        return AdminUserResponseDto.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus().name() : "ACTIVE")
                .blockedReason(user.getBlockedReason())
                .blockedAt(user.getBlockedAt())
                .emailReportEnabled(user.isEmailReportEnabled())
                .reportEmail(user.getReportEmail())
                .reportEmailVerified(user.isReportEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .level(user.getLevel())
                .totalExp(user.getTotalExp())
                .equippedBadgeId(user.getEquippedBadgeId())
                .showLevelEffects(user.isShowLevelEffects())
                .showEquippedEffect(user.isShowEquippedEffect())
                .equippedTier(user.getEquippedTier())
                .equippedEffect(user.getEquippedEffect())
                .betaTester(user.isBetaTester())
                .ownedEffectCodes(new java.util.ArrayList<>())
                .karmaPoint(user.getKarmaPoint())
                .restrictedUntil(user.getRestrictedUntil())
                .isOnline(isOnline)
                .build();
    }
}
