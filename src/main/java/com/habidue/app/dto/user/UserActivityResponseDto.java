package com.habidue.app.dto.user;

import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.dto.badge.BadgeResponseDto;
import lombok.*;

import java.util.List;

/**
 * 유저 활동 지표 및 배지 정보 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponseDto {
    private int totalPostCount;
    private int totalCommentCount;
    private int totalLikeReceivedCount;
    private int postLikeReceivedCount; // [시니어 조치] 게시글 좋아요 수
    private int commentLikeReceivedCount; // [시니어 조치] 댓글 좋아요 수
    private int totalViewReceivedCount;
    private int totalNoticeInterestCount;
    private int consecutiveAttendanceDays;
    private int totalAttendanceCount;
    private int reviewPostCount;
    private List<BadgeResponseDto> badges;
    private List<com.habidue.app.dto.badge.BadgeLevelRuleResponseDto> badgeRules; // [시니어 조치] 배지 규칙 마스터 정보 추가

    // [신규] 순위 정보 추가
    private long totalRank;
    private long totalUserCount;
    private double rankPercent;

    public static UserActivityResponseDto of(UserActivityStats stats, List<BadgeResponseDto> badges, List<com.habidue.app.dto.badge.BadgeLevelRuleResponseDto> badgeRules, long totalRank, long totalUserCount) {
        double percent = totalUserCount > 0 ? (double) totalRank / totalUserCount * 100 : 0;
        // 소수점 첫째자리까지 반올림
        percent = Math.round(percent * 10.0) / 10.0;

        return UserActivityResponseDto.builder()
                .totalPostCount(stats.getTotalPostCount())
                .totalCommentCount(stats.getTotalCommentCount())
                .totalLikeReceivedCount(stats.getPostLikeReceivedCount() + stats.getCommentLikeReceivedCount())
                .postLikeReceivedCount(stats.getPostLikeReceivedCount())
                .commentLikeReceivedCount(stats.getCommentLikeReceivedCount())
                .totalViewReceivedCount(stats.getTotalViewReceivedCount())
                .totalNoticeInterestCount(stats.getTotalNoticeInterestCount())
                .consecutiveAttendanceDays(stats.getConsecutiveAttendanceDays())
                .totalAttendanceCount(stats.getTotalAttendanceCount())
                .reviewPostCount(stats.getReviewPostCount())
                .badges(badges)
                .badgeRules(badgeRules)
                .totalRank(totalRank)
                .totalUserCount(totalUserCount)
                .rankPercent(percent)
                .build();
    }
}
