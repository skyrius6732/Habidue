package com.habidue.app.dto.ranking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankerResponseDto {
    private Long userId;
    private String nickname;
    private int level;
    private long exp; // 기간 내 획득 EXP (또는 전체 EXP)
    private int karmaPoint;
    private String rankDiff; // 순위 변동 (NEW, +2, -1, 0 등)
    private String equippedBadgeName; // [시니어] 대표 칭호 이름 추가
    private String equippedEffect; // [시니어 조치] 특수 효과 코드
    private boolean showLevelEffects; // [시니어 조치] 효과 표시 여부
    private boolean showEquippedEffect; // [시니어 조치] 이펙트 효과 표시 여부 추가
}
