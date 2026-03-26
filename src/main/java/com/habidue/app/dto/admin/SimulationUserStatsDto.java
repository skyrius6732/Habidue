package com.habidue.app.dto.admin;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.user.ExpCategory;
import com.habidue.app.domain.user.ExpHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationUserStatsDto {
    private Long userId;
    private String nickname;
    private int level;
    private long totalExp;
    
    // 분야별 EXP
    private long knowledgeExp;
    private long reviewExp;
    private long sincerityExp;

    private double karma;

    // 활동 지표 9개
    private int totalPostCount;
    private int totalCommentCount;
    private int postLikeReceivedCount;
    private int commentLikeReceivedCount;
    private int totalViewReceivedCount;
    private int totalNoticeInterestCount;
    private int consecutiveAttendanceDays;
    private int totalAttendanceCount;
    private int reviewPostCount;

    public static SimulationUserStatsDto from(User user, UserActivityStats stats, List<ExpHistory> history) {
        // 분야별 경험치 합산 로직
        Map<ExpCategory, Long> expByCat = history.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getReason().getCategory(),
                        Collectors.summingLong(ExpHistory::getAcquiredExp)
                ));

        return SimulationUserStatsDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .totalExp(user.getTotalExp())
                .knowledgeExp(expByCat.getOrDefault(ExpCategory.KNOWLEDGE, 0L))
                .reviewExp(expByCat.getOrDefault(ExpCategory.REVIEW, 0L))
                .sincerityExp(expByCat.getOrDefault(ExpCategory.SINCERITY, 0L))
                .karma((double) user.getKarmaPoint() / 10.0)
                .totalPostCount(stats.getTotalPostCount())
                .totalCommentCount(stats.getTotalCommentCount())
                .postLikeReceivedCount(stats.getPostLikeReceivedCount())
                .commentLikeReceivedCount(stats.getCommentLikeReceivedCount())
                .totalViewReceivedCount(stats.getTotalViewReceivedCount())
                .totalNoticeInterestCount(stats.getTotalNoticeInterestCount())
                .consecutiveAttendanceDays(stats.getConsecutiveAttendanceDays())
                .totalAttendanceCount(stats.getTotalAttendanceCount())
                .reviewPostCount(stats.getReviewPostCount())
                .build();
    }
}
