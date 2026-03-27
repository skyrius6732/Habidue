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
}
