package com.habidue.app.dto.badge;

import com.habidue.app.domain.badge.Badge;
import com.habidue.app.domain.badge.UserBadge;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 배지 정보 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponseDto {
    private Long id;
    private String code;
    private String displayName;
    private String description;
    private String type;
    private int level;
    private String badgeTip; // [시니어 조치] 가이드 팁 전달
    private boolean isRepresentative;
    private LocalDateTime acquiredAt;
    
    // [시니어] 진행도 표시를 위한 필드 추가
    private int currentValue;      // 현재 활동 수치 (예: 43)
    private Integer nextTargetValue; // 다음 레벨 목표 수치 (예: 50, 만렙 시 null)

    // [시니어] 하위 호환성을 위한 2인자 메서드 유지
    public static BadgeResponseDto from(UserBadge userBadge, String realTimeName) {
        return from(userBadge, realTimeName, 0, null);
    }

    public static BadgeResponseDto from(UserBadge userBadge, String realTimeName, int currentValue, Integer nextTargetValue) {
        Badge badge = userBadge.getBadge();
        return BadgeResponseDto.builder()
                .id(badge.getId())
                .code(badge.getCode())
                .displayName(realTimeName != null ? realTimeName : badge.getName())
                .description(badge.getDescription())
                .type(badge.getType())
                .level(userBadge.getLevel())
                .badgeTip(badge.getBadgeTip())
                .isRepresentative(userBadge.isRepresentative())
                .acquiredAt(userBadge.getAcquiredAt())
                .currentValue(currentValue)
                .nextTargetValue(nextTargetValue)
                .build();
    }

}
