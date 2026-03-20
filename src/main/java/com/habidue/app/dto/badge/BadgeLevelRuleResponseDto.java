package com.habidue.app.dto.badge;

import com.habidue.app.domain.badge.BadgeLevelRule;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeLevelRuleResponseDto {
    private String badgeType;
    private int level;
    private int requiredValue;
    private String rankEmoji;
    private String rankTitle;
    private String categoryName;
    private String fullDisplayName;

    public static BadgeLevelRuleResponseDto from(BadgeLevelRule rule) {
        return BadgeLevelRuleResponseDto.builder()
                .badgeType(rule.getBadgeType())
                .level(rule.getLevel())
                .requiredValue(rule.getRequiredValue())
                .rankEmoji(rule.getRankEmoji())
                .rankTitle(rule.getRankTitle())
                .categoryName(rule.getCategoryName())
                .fullDisplayName(rule.getFullDisplayName())
                .build();
    }
}
