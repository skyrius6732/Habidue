package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.board.ReportRequestDto;
import com.habidue.app.service.board.BoardReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final BoardReportService reportService;

    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> report(@Valid @RequestBody ReportRequestDto requestDto) {
        reportService.report(requestDto);
        return ApiResponse.success(HttpStatus.CREATED, null);
    }
}
