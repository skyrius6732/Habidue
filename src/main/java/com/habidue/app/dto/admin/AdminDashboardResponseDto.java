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
    private Map<String, Long> countBySource; // LH, SH, PRIVATE 등
    private Map<String, Long> countByStatus; // 접수중, 마감 등
    private long totalUsers;
    private long todayUsers;
    private Map<String, Long> topKeywords; // 인기 키워드 TOP 5 (예정)
}
