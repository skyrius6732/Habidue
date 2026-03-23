package com.habidue.app.controller.admin;

import com.habidue.app.domain.board.ReportStatus;
import com.habidue.app.domain.board.ReportTargetType;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.ConversationReportGroupDto;
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.service.board.AdminBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/board")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class AdminBoardController {

    private final AdminBoardService adminBoardService;

    /**
     * 게시글 관리 목록 조회
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPosts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(adminBoardService.getAdminPosts(userId, keyword, status, pageable));
    }

    /**
     * 게시글 상태 변경 (블라인드/복구)
     */
    @PatchMapping("/posts/{postId}/status")
    public ResponseEntity<ApiResponse<Void>> changePostStatus(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        adminBoardService.changePostStatus(postId, body.get("status"));
        return ApiResponse.success(null);
    }

    /**
     * 게시글 강제 수정
     */
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        adminBoardService.updatePost(postId, body.get("title"), body.get("content"));
        return ApiResponse.success(null);
    }

    /**
     * 댓글 관리 목록 조회
     */
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponseDto>>> getComments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(adminBoardService.getAdminComments(userId, keyword, status, pageable));
    }

    /**
     * 댓글 상태 변경 (블라인드/복구)
     */
    @PatchMapping("/comments/{commentId}/status")
    public ResponseEntity<ApiResponse<Void>> changeCommentStatus(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body) {
        adminBoardService.changeCommentStatus(commentId, body.get("status"));
        return ApiResponse.success(null);
    }

    /**
     * 댓글 강제 수정
     */
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body) {
        adminBoardService.updateComment(commentId, body.get("content"));
        return ApiResponse.success(null);
    }

    /**
     * [시니어 조치] 신고 목록 조회
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<ReportAdminResponseDto>>> getAdminReports(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ApiResponse.success(adminBoardService.getAdminReports(targetType, status, keyword, pageable));
    }

    /**
     * [시니어 조치] 쪽지 신고 그룹화 조회 (Split Card UI 전용)
     */
    @GetMapping("/reports/grouped-messages")
    public ResponseEntity<ApiResponse<List<ConversationReportGroupDto>>> getGroupedMessageReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminBoardService.getGroupedMessageReports(status, keyword));
    }


    /**
     * [시니어 조치] 신고 단건 처리
     */
    @PatchMapping("/reports/handle")
    public ResponseEntity<ApiResponse<Void>> handleReport(
            @RequestBody Map<String, Object> body) {
        Long targetId = Long.valueOf(body.get("targetId").toString());
        ReportTargetType targetType = ReportTargetType.valueOf(body.get("targetType").toString());
        ReportStatus status = ReportStatus.valueOf(body.get("status").toString());
        
        adminBoardService.handleReport(targetId, targetType, status);
        return ApiResponse.success(null);
    }

    /**
     * [시니어 조치] 신고 일괄 처리 (일괄 블라인드 등)
     */
    @PostMapping("/reports/bulk-handle")
    public ResponseEntity<ApiResponse<Void>> bulkHandleReports(
            @RequestBody Map<String, Object> body) {
        List<Long> targetIds = (List<Long>) body.get("targetIds");
        ReportTargetType targetType = ReportTargetType.valueOf(body.get("targetType").toString());
        ReportStatus status = ReportStatus.valueOf(body.get("status").toString());

        adminBoardService.bulkHandleReports(targetIds, targetType, status);
        return ApiResponse.success(null);
    }
}
