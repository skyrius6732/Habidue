package com.habidue.app.controller.admin;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.AdminDashboardResponseDto;
import com.habidue.app.dto.notice.NoticeRequestDto;
import com.habidue.app.dto.notice.NoticeResponseDto;
import com.habidue.app.service.admin.AdminDashboardService;
import com.habidue.app.service.notice.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class NoticeAdminController {

    private final AdminDashboardService dashboardService;
    private final NoticeService noticeService;
    private final com.habidue.app.service.tag.TagService tagService;

    @PostMapping("/notices/reprocess-tags")
    public ResponseEntity<ApiResponse<Void>> reprocessAllTags() {
        tagService.reprocessAllNotices();
        return ApiResponse.success(null);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AdminDashboardResponseDto>> getStats() {
        return ApiResponse.success(dashboardService.getDashboardStats());
    }

    @GetMapping("/notices")
    public ResponseEntity<ApiResponse<Page<NoticeResponseDto>>> getNotices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> sources,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Boolean isNew, // 추가
            Pageable pageable) {
        // Let's use searchNotices but pass null for user to see all
        Page<Notice> notices = noticeService.searchNotices(keyword, sources, statuses, "LATEST", null, null, false, null, isNew, pageable);
        return ApiResponse.success(notices.map(notice -> new NoticeResponseDto(notice)));
    }

    @PostMapping("/notices")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> createNotice(@RequestBody NoticeRequestDto dto) {
        return ApiResponse.success(new NoticeResponseDto(noticeService.createNotice(dto)));
    }

    @PutMapping("/notices/{id}")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> updateNotice(@PathVariable Long id, @RequestBody NoticeRequestDto dto) {
        return ApiResponse.success(new NoticeResponseDto(noticeService.updateNotice(id, dto)));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/notices/bulk")
    public ResponseEntity<ApiResponse<Void>> deleteNoticesBulk(@RequestBody List<Long> ids) {
        noticeService.deleteNoticesInBulk(ids);
        return ApiResponse.success(null);
    }

    @PatchMapping("/notices/{id}/status")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> updateNoticeStatus(@PathVariable Long id, @RequestParam NoticeStatus status) {
        // [시니어 조치] 관리자 작업이므로 실시간 랭킹 점수 합산에서 제외 (null 전달)
        Notice notice = noticeService.getNotice(id, null);
        notice.setStatus(status);
        
        // [시니어 조치] 상태 변경 시 태그 재분류 및 알림 발송 (TagService 내부에서 이벤트 발행)
        tagService.autoClassifyAndAddTags(notice, true);
        
        return ApiResponse.success(new NoticeResponseDto(noticeService.save(notice)));
    }
}
