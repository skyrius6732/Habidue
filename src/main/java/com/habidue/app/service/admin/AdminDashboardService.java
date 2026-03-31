package com.habidue.app.service.admin;

import com.habidue.app.domain.board.ReportStatus;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.dto.admin.AdminDashboardResponseDto;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public AdminDashboardResponseDto getDashboardStats() {
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        long totalNotices = noticeRepository.count();
        long todayNotices = noticeRepository.countRecent(startOfToday);
        long recruitingNotices = noticeRepository.countByStatus(NoticeStatus.RECRUITING);

        Map<String, Long> countBySource = listToMap(noticeRepository.getCountBySource());
        Map<String, Long> countByStatus = listToMap(noticeRepository.getCountByStatus());

        long totalUsers = userRepository.count();
        long todayUsers = userRepository.countRecent(startOfToday);

        Map<String, Long> countByReportStatus = listToMap(reportRepository.getCountByStatus());
        Map<String, Long> countByUserStatus = listToMap(userRepository.getCountByStatus());
        Map<String, Long> countByPostStatus = listToMap(postRepository.getCountByPostStatus());
        long pendingReports = reportRepository.countByStatus(ReportStatus.WAITING);

        // [시니어 추가] 트렌드 데이터 생성
        AdminDashboardResponseDto.TrendData trends = AdminDashboardResponseDto.TrendData.builder()
                .dailyNotices(getTrendData("notices", "announcement_date", "DATE", 14))
                .dailyUsers(getTrendData("users", "created_at", "DATE", 14))
                .weeklyNotices(getTrendData("notices", "announcement_date", "WEEK", 8))
                .weeklyUsers(getTrendData("users", "created_at", "WEEK", 8))
                .monthlyNotices(getTrendData("notices", "announcement_date", "MONTH", 6))
                .monthlyUsers(getTrendData("users", "created_at", "MONTH", 6))
                .build();

        return AdminDashboardResponseDto.builder()
                .totalNotices(totalNotices)
                .todayNotices(todayNotices)
                .recruitingNotices(recruitingNotices)
                .countBySource(countBySource)
                .countByStatus(countByStatus)
                .totalUsers(totalUsers)
                .todayUsers(todayUsers)
                .topKeywords(new HashMap<>())
                .countByReportStatus(countByReportStatus)
                .countByUserStatus(countByUserStatus)
                .countByPostStatus(countByPostStatus)
                .pendingReports(pendingReports)
                .trends(trends)
                .build();
    }

    /**
     * [시니어 조치] 범용 시계열 통계 추출 메서드 (Native Query 사용)
     */
    private Map<String, Long> getTrendData(String table, String dateColumn, String type, int limit) {
        String dateFormat = type.equals("MONTH") ? "%Y-%m" : type.equals("WEEK") ? "%Y-%u" : "%Y-%m-%d";
        String intervalUnit = type.equals("MONTH") ? "MONTH" : type.equals("WEEK") ? "WEEK" : "DAY";
        int intervalVal = type.equals("MONTH") ? 12 : type.equals("WEEK") ? 12 : 30; // 넉넉하게 최근 데이터 필터링

        String sql = "SELECT DATE_FORMAT(" + dateColumn + ", '" + dateFormat + "') as date_key, COUNT(*) as cnt " +
                     "FROM " + table + " " +
                     "WHERE " + dateColumn + " >= DATE_SUB(NOW(), INTERVAL " + intervalVal + " " + intervalUnit + ") " +
                     "GROUP BY date_key " +
                     "ORDER BY date_key DESC " +
                     "LIMIT " + limit;

        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();
        Map<String, Long> map = new java.util.LinkedHashMap<>();
        
        // 내림차순으로 가져온 뒤, 그래프 표시를 위해 다시 오름차순으로 정렬하여 맵에 저장
        for (int i = results.size() - 1; i >= 0; i--) {
            Object[] row = results.get(i);
            map.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<String, Long> listToMap(List<Object[]> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        obj -> obj[0].toString(),
                        obj -> (Long) obj[1]
                ));
    }
}
