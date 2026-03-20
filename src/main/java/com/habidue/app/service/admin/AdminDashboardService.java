package com.habidue.app.service.admin;

import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.dto.admin.AdminDashboardResponseDto;
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

    public AdminDashboardResponseDto getDashboardStats() {
        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);

        long totalNotices = noticeRepository.count();
        long todayNotices = noticeRepository.countRecent(startOfToday);
        long recruitingNotices = noticeRepository.countByStatus(NoticeStatus.RECRUITING);

        Map<String, Long> countBySource = listToMap(noticeRepository.getCountBySource());
        Map<String, Long> countByStatus = listToMap(noticeRepository.getCountByStatus());

        long totalUsers = userRepository.count();
        long todayUsers = userRepository.countRecent(startOfToday);

        return AdminDashboardResponseDto.builder()
                .totalNotices(totalNotices)
                .todayNotices(todayNotices)
                .recruitingNotices(recruitingNotices)
                .countBySource(countBySource)
                .countByStatus(countByStatus)
                .totalUsers(totalUsers)
                .todayUsers(todayUsers)
                .topKeywords(new HashMap<>()) // To be implemented later
                .build();
    }

    private Map<String, Long> listToMap(List<Object[]> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        obj -> obj[0].toString(),
                        obj -> (Long) obj[1]
                ));
    }
}
