package com.habidue.app.repository.user;

import com.habidue.app.domain.user.Attendance;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // 특정 유저의 특정 날짜 출석 기록 조회
    Optional<Attendance> findByUserAndAttendanceDate(User user, LocalDate attendanceDate);
    
    // 특정 유저의 특정 기간 내 출석 기록 조회
    List<Attendance> findByUserAndAttendanceDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    // 특정 유저가 오늘 출석했는지 확인용
    boolean existsByUserAndAttendanceDate(User user, LocalDate attendanceDate);

    void deleteByUser(User user);
}
