package com.habidue.app.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AdminDashboardResponseDto {
    private long totalNotices;
    private long todayNotices;
    private long recruitingNotices;
    private Map<String, Long> countBySource;
    private Map<String, Long> countByStatus;
    private long totalUsers;
    private long todayUsers;
    private Map<String, Long> topKeywords;
    private Map<String, Long> countByReportStatus;
    private Map<String, Long> countByUserStatus;
    private Map<String, Long> countByPostStatus;
    private long pendingReports;

    // [시니어 추가] 시계열 트렌드 데이터
    private TrendData trends;

    @Getter
    @Builder
    public static class TrendData {
        private Map<String, Long> dailyNotices;
        private Map<String, Long> dailyUsers;
        private Map<String, Long> weeklyNotices;
        private Map<String, Long> weeklyUsers;
        private Map<String, Long> monthlyNotices;
        private Map<String, Long> monthlyUsers;
    }
}
