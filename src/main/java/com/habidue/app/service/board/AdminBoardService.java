package com.habidue.app.service.board;

import com.habidue.app.domain.board.*;
import com.habidue.app.domain.user.KarmaReason;
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.service.user.KarmaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final KarmaService karmaService;

    /**
     * 게시글 목록 조회
     */
    public Page<PostResponseDto> getAdminPosts(Long userId, String keyword, String status, Pageable pageable) {
        if (userId != null) {
            return postRepository.findPostsByAuthor(userId, keyword, status, pageable)
                    .map(PostResponseDto::from);
        }
        return postRepository.findPosts(null, null, null, null, keyword, status, null, null, pageable, null, true)
                .map(PostResponseDto::from);
    }

    /**
     * 게시글 상태 변경 시 신고 내역 자동 동기화
     */
    @Transactional
    public void changePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        
        String oldStatus = post.getStatus();
        post.changeStatus(status);

        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.BLIND_COMPLETE);
            // [시니어] 상태가 'BLINDED'로 새로 '변경'될 때만 카르마 차감
            karmaService.deductKarma(post.getAuthor().getId(), 20, KarmaReason.REPORT_APPROVED, 
                    "관리자에 의한 게시글 블라인드 (ID: " + postId + ")", null, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.BLIND_COMPLETE);
            // [시니어 조치] 'DELETED'는 가장 강력한 제재 (작성자에게도 비노출)
            karmaService.deductKarma(post.getAuthor().getId(), 50, KarmaReason.POST_DELETED, 
                    "심각한 운영원칙 위반으로 인한 게시글 삭제 (ID: " + postId + ")", null, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            // [시니어 조치] 복구 시에는 대기 상태가 아닌 반려(무시) 상태로 두어 다시 신고 목록에 뜨지 않게 할 수도 있고, 
            // 대기로 두어 재검토하게 할 수도 있습니다. 여기서는 사용자 요청에 맞춰 싱크를 유지합니다.
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.REJECTED);
        }
    }

    /**
     * 게시글 내용 강제 수정
     */
    @Transactional
    public void updatePost(Long postId, String title, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        post.update(title, content, post.getCategory(), post.getSubCategory(), post.getRegionTag());
    }

    /**
     * 댓글 목록 조회
     */
    public Page<CommentResponseDto> getAdminComments(Long userId, String keyword, String status, Pageable pageable) {
        return commentRepository.findComments(userId, keyword, status, pageable)
                .map(comment -> CommentResponseDto.from(comment, true)); // 관리자용 원본 노출
    }

    /**
     * 댓글 상태 변경 시 신고 내역 자동 동기화
     */
    @Transactional
    public void changeCommentStatus(Long commentId, String status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        
        String oldStatus = comment.getStatus();
        comment.changeStatus(status);

        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.BLIND_COMPLETE);
            // [시니어] 상태가 'BLINDED'로 새로 '변경'될 때만 카르마 차감
            karmaService.deductKarma(comment.getAuthor().getId(), 10, KarmaReason.REPORT_APPROVED, 
                    "관리자에 의한 댓글 블라인드 (ID: " + commentId + ")", null, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.BLIND_COMPLETE);
            // [시니어 조치] 댓글 영구 삭제 시 카르마 차감
            karmaService.deductKarma(comment.getAuthor().getId(), 30, KarmaReason.COMMENT_DELETED, 
                    "심각한 운영원칙 위반으로 인한 댓글 삭제 (ID: " + commentId + ")", null, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.REJECTED);
        }
    }

    /**
     * 댓글 내용 강제 수정
     */
    @Transactional
    public void updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        comment.updateContent(content);
    }

    /**
     * 신고 목록 조회
     */
    public Page<ReportAdminResponseDto> getAdminReports(ReportTargetType targetType, String status, String keyword, Pageable pageable) {
        return reportRepository.findGroupedReports(targetType, status, keyword, pageable);
    }

    /**
     * [시니어 조치] 반려 및 복구 시 상태 동기화 완성
     * [패널티 고도화] 신고 처리 시 작성자 카르마 점수 차감 연동
     */
    @Transactional
    public void handleReport(Long targetId, ReportTargetType targetType, ReportStatus status) {
        // 1. 신고 상태 변경
        syncReportStatus(targetId, targetType, status);

        // 2. 결정된 상태에 따른 게시글/댓글 상태 동기화
        if (targetType == ReportTargetType.POST) {
            postRepository.findById(targetId).ifPresent(p -> {
                String oldStatus = p.getStatus();
                
                // 블라인드 처리
                if (status == ReportStatus.BLIND_COMPLETE) {
                    if (!"BLINDED".equalsIgnoreCase(oldStatus)) {
                        p.changeStatus("BLINDED");
                        karmaService.deductKarma(p.getAuthor().getId(), 20, KarmaReason.REPORT_APPROVED, 
                                "신고 승인에 따른 게시글 블라인드 (ID: " + targetId + ")", null, true);
                    }
                } 
                // 영구 삭제 처리
                else if (status == ReportStatus.DELETE_COMPLETE) {
                    if (!"DELETED".equalsIgnoreCase(oldStatus)) {
                        p.changeStatus("DELETED");
                        karmaService.deductKarma(p.getAuthor().getId(), 50, KarmaReason.POST_DELETED, 
                                "신고 승인에 따른 게시글 영구 삭제 (ID: " + targetId + ")", null, true);
                    }
                }
                // 반려 또는 복구(대기중) -> ACTIVE로 복구
                else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                    if (!"ACTIVE".equalsIgnoreCase(oldStatus)) {
                        p.changeStatus("ACTIVE");
                    }
                }
            });
        } else if (targetType == ReportTargetType.COMMENT) {
            commentRepository.findById(targetId).ifPresent(c -> {
                String oldStatus = c.getStatus();

                // 블라인드 처리
                if (status == ReportStatus.BLIND_COMPLETE) {
                    if (!"BLINDED".equalsIgnoreCase(oldStatus)) {
                        c.changeStatus("BLINDED");
                        karmaService.deductKarma(c.getAuthor().getId(), 10, KarmaReason.REPORT_APPROVED, 
                                "신고 승인에 따른 댓글 블라인드 (ID: " + targetId + ")", null, true);
                    }
                }
                // 영구 삭제 처리
                else if (status == ReportStatus.DELETE_COMPLETE) {
                    if (!"DELETED".equalsIgnoreCase(oldStatus)) {
                        c.changeStatus("DELETED");
                        karmaService.deductKarma(c.getAuthor().getId(), 30, KarmaReason.COMMENT_DELETED, 
                                "신고 승인에 따른 댓글 영구 삭제 (ID: " + targetId + ")", null, true);
                    }
                }
                // 반려 또는 복구(대기중) -> ACTIVE로 복구
                else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                    if (!"ACTIVE".equalsIgnoreCase(oldStatus)) {
                        c.changeStatus("ACTIVE");
                    }
                }
            });
        }
    }

    @Transactional
    public void bulkHandleReports(List<?> rawTargetIds, ReportTargetType targetType, ReportStatus status) {
        for (Object rawId : rawTargetIds) {
            Long targetId = Long.valueOf(rawId.toString());
            handleReport(targetId, targetType, status);
        }
    }

    private void syncReportStatus(Long targetId, ReportTargetType targetType, ReportStatus status) {
        List<Report> reports = reportRepository.findAllByTargetIdAndTargetType(targetId, targetType);
        for (Report report : reports) {
            report.changeStatus(status);
        }
    }
}
