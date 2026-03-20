package com.habidue.app.service.user;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.user.Attendance;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.repository.user.AttendanceRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.service.badge.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.habidue.app.service.user.ExpService;
import com.habidue.app.domain.user.ExpReason;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final BadgeService badgeService;
    private final ExpService expService; // [시니어 조치] EXP 연동

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new NoSuchElementException("인증된 사용자 정보를 DB에서 찾을 수 없습니다."));
    }

    @Transactional
    public Attendance checkIn(String memo) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByUserAndAttendanceDate(user, today)) {
            throw new IllegalStateException("오늘은 이미 출석하셨습니다.");
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .attendanceDate(today)
                .memo(memo)
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);

        // [시니어 조치] 연속 출석 통계 업데이트 및 배지 체크
        UserActivityStats stats = userActivityStatsRepository.findById(user.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(user)));

        LocalDate yesterday = today.minusDays(1);
        if (stats.getLastAttendanceDate() != null && stats.getLastAttendanceDate().equals(yesterday)) {
            stats.setConsecutiveAttendanceDays(stats.getConsecutiveAttendanceDays() + 1);
        } else {
            stats.setConsecutiveAttendanceDays(1);
        }
        stats.incrementAttendanceCount(); // 누적 출석수 증가
        stats.setLastAttendanceDate(today);
        userActivityStatsRepository.save(stats);
        
        badgeService.checkAndAwardBadges(stats); // 배지 엔진 가동
        
        // [시니어 조치] 출석 EXP 부여
        expService.grantExp(user.getId(), ExpReason.ATTENDANCE, "출석 체크");

        return savedAttendance;
    }

    public List<LocalDate> getAttendanceDates(int year, int month) {
        User user = getCurrentUser();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepository.findByUserAndAttendanceDateBetween(user, startDate, endDate)
                .stream()
                .map(Attendance::getAttendanceDate)
                .collect(Collectors.toList());
    }

    public boolean isCheckedToday() {
        try {
            User user = getCurrentUser();
            return attendanceRepository.existsByUserAndAttendanceDate(user, LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }
}
