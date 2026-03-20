package com.habidue.app.dto.admin;

import lombok.*;

/**
 * 관리자용 배지 규칙 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeLevelRuleRequestDto {
    private int requiredValue;
    private String rankEmoji;
    private String rankTitle;
    private String categoryName;
    private String description;
    private String badgeTip;
}
