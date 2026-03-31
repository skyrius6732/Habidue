package com.habidue.app.controller.admin;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.AdminDashboardResponseDto;
import com.habidue.app.dto.notice.NoticeRequestDto;
import com.habidue.app.dto.notice.NoticeResponseDto;
import com.habidue.app.service.admin.AdminDashboardService;
import com.habidue.app.service.notice.NoticeService;
import com.habidue.app.service.notice.collector.lh.LhNoticeCollectorService;
import com.habidue.app.service.notice.collector.sh.ShNoticeCrawlerService;
import com.habidue.app.service.notice.collector.civil.PrivateNoticeCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class NoticeAdminController {

    private final AdminDashboardService dashboardService;
    private final NoticeService noticeService;
    private final com.habidue.app.service.tag.TagService tagService;
    private final LhNoticeCollectorService lhNoticeCollectorService;
    private final ShNoticeCrawlerService shNoticeCrawlerService;
    private final PrivateNoticeCrawlerService privateNoticeCrawlerService;

    /**
     * 운영 초기 1회용 전체 수집 트리거 (백그라운드 실행)
     */
    @PostMapping("/notices/collect/lh")
    public ResponseEntity<ApiResponse<String>> triggerLhFullCollect() {
        if (lhNoticeCollectorService.isRunning()) {
            return ApiResponse.success("이미 수집 중입니다.");
        }
        Thread thread = new Thread(lhNoticeCollectorService::collectAllLhNotices);
        thread.setDaemon(true);
        thread.start();
        return ApiResponse.success("LH 전체 수집을 시작했습니다. 서버 로그를 확인하세요.");
    }

    @PostMapping("/notices/collect/lh/stop")
    public ResponseEntity<ApiResponse<String>> stopLhCollect() {
        lhNoticeCollectorService.stopCollection();
        return ApiResponse.success("수집 중단 요청을 보냈습니다.");
    }

    @GetMapping("/notices/collect/lh/status")
    public ResponseEntity<ApiResponse<Boolean>> getLhCollectStatus() {
        return ApiResponse.success(lhNoticeCollectorService.isRunning());
    }

    @PostMapping("/notices/collect/sh")
    public ResponseEntity<ApiResponse<String>> triggerShFullCollect() {
        if (shNoticeCrawlerService.isRunning()) {
            return ApiResponse.success("이미 수집 중입니다.");
        }
        Thread thread = new Thread(shNoticeCrawlerService::crawlAllShNotices);
        thread.setDaemon(true);
        thread.start();
        return ApiResponse.success("SH 전체 수집을 시작했습니다. 서버 로그를 확인하세요.");
    }

    @PostMapping("/notices/collect/sh/stop")
    public ResponseEntity<ApiResponse<String>> stopShCollect() {
        shNoticeCrawlerService.stopCollection();
        return ApiResponse.success("수집 중단 요청을 보냈습니다.");
    }

    @GetMapping("/notices/collect/sh/status")
    public ResponseEntity<ApiResponse<Boolean>> getShCollectStatus() {
        return ApiResponse.success(shNoticeCrawlerService.isRunning());
    }

    @PostMapping("/notices/collect/private")
    public ResponseEntity<ApiResponse<String>> triggerPrivateFullCollect() {
        if (privateNoticeCrawlerService.isRunning()) {
            return ApiResponse.success("이미 수집 중입니다.");
        }
        Thread thread = new Thread(privateNoticeCrawlerService::crawlAllPrivateNotices);
        thread.setDaemon(true);
        thread.start();
        return ApiResponse.success("민간임대 전체 수집을 시작했습니다. 서버 로그를 확인하세요.");
    }

    @PostMapping("/notices/collect/private/stop")
    public ResponseEntity<ApiResponse<String>> stopPrivateCollect() {
        privateNoticeCrawlerService.stopCollection();
        return ApiResponse.success("수집 중단 요청을 보냈습니다.");
    }

    @GetMapping("/notices/collect/private/status")
    public ResponseEntity<ApiResponse<Boolean>> getPrivateCollectStatus() {
        return ApiResponse.success(privateNoticeCrawlerService.isRunning());
    }

    @PostMapping("/notices/reprocess-tags")
    public ResponseEntity<ApiResponse<String>> reprocessAllTags() {
        // [시니어 조치] 대량의 데이터 처리이므로 비동기로 실행하여 사용자 타임아웃 방지
        Thread thread = new Thread(() -> {
            try {
                tagService.reprocessAllNotices();
            } catch (Exception e) {
                log.error("태그 전체 재설정 중 오류 발생: ", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        return ApiResponse.success("태그 재설정 작업을 백그라운드에서 시작했습니다.");
    }

    /**
     * 모든 백그라운드 작업(수집, 태그 재설정)의 진행률 통합 조회
     */
    @GetMapping("/notices/tasks/status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAllTasksStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        boolean lhRunning = lhNoticeCollectorService.isRunning();
        boolean shRunning = shNoticeCrawlerService.isRunning();
        boolean privateRunning = privateNoticeCrawlerService.isRunning();
        boolean tagsRunning = tagService.isProcessing();

        status.put("lh", java.util.Map.of(
            "isRunning", lhRunning,
            "current", lhNoticeCollectorService.getProcessedPages(),
            "total", lhNoticeCollectorService.getTotalPages()
        ));
        
        status.put("sh", java.util.Map.of(
            "isRunning", shRunning,
            "current", shNoticeCrawlerService.getProcessedPages(),
            "total", shNoticeCrawlerService.getTotalPages()
        ));
        
        status.put("private", java.util.Map.of(
            "isRunning", privateRunning,
            "current", privateNoticeCrawlerService.getProcessedPages(),
            "total", privateNoticeCrawlerService.getTotalPages()
        ));
        
        status.put("tags", java.util.Map.of(
            "isRunning", tagsRunning,
            "current", tagService.getProcessedCount(),
            "total", tagService.getTotalCount()
        ));
        
        if (lhRunning || shRunning || privateRunning || tagsRunning) {
            log.debug("백그라운드 작업 진행 중: lh={}, sh={}, private={}, tags={}", lhRunning, shRunning, privateRunning, tagsRunning);
        }
        
        return ApiResponse.success(status);
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
