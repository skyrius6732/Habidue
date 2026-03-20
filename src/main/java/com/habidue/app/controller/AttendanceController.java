package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.user.AttendanceRequestDto;
import com.habidue.app.service.user.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> checkIn(@RequestBody(required = false) AttendanceRequestDto requestDto) {
        String memo = (requestDto != null) ? requestDto.getMemo() : null;
        attendanceService.checkIn(memo);
        return ApiResponse.success(HttpStatus.CREATED, null);
    }

    @GetMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getMyAttendance(
            @RequestParam int year,
            @RequestParam int month) {
        List<LocalDate> dates = attendanceService.getAttendanceDates(year, month);
        return ApiResponse.success(dates);
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Boolean>> isCheckedToday() {
        boolean isChecked = attendanceService.isCheckedToday();
        return ApiResponse.success(isChecked);
    }
}
