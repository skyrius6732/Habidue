package com.habidue.app.service.board;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.*;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.user.KarmaReason;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import com.habidue.app.dto.admin.ConversationReportGroupDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.repository.message.MessageRepository;
import com.habidue.app.service.user.KarmaService;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final KarmaService karmaService;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getAdminPosts(Long userId, String keyword, String status, Pageable pageable) {
        if (userId != null) {
            return postRepository.findPostsByAuthor(userId, keyword, status, pageable).map(PostResponseDto::from);
        }
        return postRepository.findPosts(null, null, null, null, keyword, status, null, null, pageable, null, true)
                .map(PostResponseDto::from);
    }

    @Transactional
    public void changePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        String oldStatus = post.getStatus();
        User author = post.getAuthor();
        
        post.changeStatus(status);
        
        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(author.getId(), 20, KarmaReason.REPORT_POST_APPROVED, "관리자에 의한 게시글 블라인드 (ID: " + postId + ")", null, true);
            notificationService.send(author, NotificationType.SYSTEM, "⚠️ 작성하신 게시글이 블라인드 처리되었습니다: \"" + post.getTitle() + "\"", post.getId(), post.getId());
            notifyReporters(postId, ReportTargetType.POST, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.DELETE_COMPLETE);
            karmaService.deductKarma(author.getId(), 50, KarmaReason.POST_DELETED, "심각한 운영원칙 위반으로 인한 게시글 삭제 (ID: " + postId + ")", null, true);
            notificationService.send(author, NotificationType.SYSTEM, "🚫 작성하신 게시글이 운영 정책 위반으로 영구 삭제되었습니다: \"" + post.getTitle() + "\"", null, null);
            notifyReporters(postId, ReportTargetType.POST, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.REJECTED);
            notifyReporters(postId, ReportTargetType.POST, false);
        }
    }

    @Transactional
    public void updatePost(Long postId, String title, String content) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        post.update(title, content, post.getCategory(), post.getSubCategory(), post.getRegionTag());
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getAdminComments(Long userId, String keyword, String status, Pageable pageable) {
        return commentRepository.findComments(userId, keyword, status, pageable).map(comment -> {
            CommentResponseDto dto = CommentResponseDto.from(comment, true);
            dto.setChildren(new java.util.ArrayList<>());
            return dto;
        });
    }

    @Transactional
    public void changeCommentStatus(Long commentId, String status) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        String oldStatus = comment.getStatus();
        User author = comment.getAuthor();
        
        comment.changeStatus(status);
        String preview = comment.getContent().length() > 25 ? comment.getContent().substring(0, 25) + "..." : comment.getContent();

        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(author.getId(), 10, KarmaReason.REPORT_COMMENT_APPROVED, "관리자에 의한 댓글 블라인드 (ID: " + commentId + ")", null, true);
            notificationService.send(author, NotificationType.SYSTEM, "⚠️ 작성하신 댓글이 블라인드 처리되었습니다: \"" + preview + "\"", comment.getId(), comment.getPost().getId());
            notifyReporters(commentId, ReportTargetType.COMMENT, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.DELETE_COMPLETE);
            karmaService.deductKarma(author.getId(), 30, KarmaReason.COMMENT_DELETED, "심각한 운영원칙 위반으로 인한 댓글 삭제 (ID: " + commentId + ")", null, true);
            notificationService.send(author, NotificationType.SYSTEM, "🚫 작성하신 댓글이 운영 정책 위반으로 영구 삭제되었습니다: \"" + preview + "\"", null, null);
            notifyReporters(commentId, ReportTargetType.COMMENT, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.REJECTED);
            notifyReporters(commentId, ReportTargetType.COMMENT, false);
        }
    }

    @Transactional
    public void updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        comment.updateContent(content);
    }

    @Transactional(readOnly = true)
    public Page<ReportAdminResponseDto> getAdminReports(ReportTargetType targetType, String status, String keyword, Pageable pageable) {
        return reportRepository.findGroupedReports(targetType, status, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<ConversationReportGroupDto> getGroupedMessageReports(String status, String keyword) {
        Page<ReportAdminResponseDto> allReports = reportRepository.findGroupedReports(ReportTargetType.MESSAGE, status, keyword, Pageable.unpaged());
        java.util.Map<String, ConversationReportGroupDto> groupedMap = new java.util.LinkedHashMap<>();

        for (ReportAdminResponseDto report : allReports.getContent()) {
            com.habidue.app.domain.message.Message m = messageRepository.findById(report.getTargetId()).orElse(null);
            if (m == null || report.getReporters().isEmpty()) continue;

            User sender = m.getSender();
            User receiver = m.getReceiver();
            Long u1Id = Math.min(sender.getId(), receiver.getId());
            Long u2Id = Math.max(sender.getId(), receiver.getId());
            String convKey = u1Id + "-" + u2Id;

            ConversationReportGroupDto group = groupedMap.getOrDefault(convKey, ConversationReportGroupDto.builder().conversationId(convKey).build());
            if (group.getUser1Name() == null) {
                User u1 = sender.getId().equals(u1Id) ? sender : receiver;
                group.setUser1Name(u1.getNickname());
                group.setUser2Name((sender.getId().equals(u2Id) ? sender : receiver).getNickname());
            }

            Long reporterId = report.getReporters().get(0).getReporterId();
            if (reporterId.equals(u1Id)) {
                if (group.getUser1Report() == null || group.getUser1Report().getReportStatus().equals("REJECTED")) group.setUser1Report(report);
            } else if (reporterId.equals(u2Id)) {
                if (group.getUser2Report() == null || group.getUser2Report().getReportStatus().equals("REJECTED")) group.setUser2Report(report);
            }
            groupedMap.put(convKey, group);
        }
        return new java.util.ArrayList<>(groupedMap.values());
    }

    @Transactional
    public void handleReport(Long targetId, ReportTargetType targetType, ReportStatus status) {
        List<Report> existingReports = reportRepository.findAllByTargetIdAndTargetType(targetId, targetType);
        ReportStatus oldStatus = existingReports.isEmpty() ? ReportStatus.WAITING : existingReports.get(0).getStatus();
        
        if (targetType == ReportTargetType.POST) {
            handlePostSanction(targetId, status, oldStatus);
        } else if (targetType == ReportTargetType.COMMENT) {
            handleCommentSanction(targetId, status, oldStatus);
        } else if (targetType == ReportTargetType.MESSAGE) {
            handleMessageSanction(targetId, status, oldStatus);
        }
    }

    private void handlePostSanction(Long targetId, ReportStatus status, ReportStatus oldStatus) {
        syncReportStatus(targetId, ReportTargetType.POST, status);
        postRepository.findById(targetId).ifPresent(p -> {
            User author = p.getAuthor();
            if (status == ReportStatus.BLIND_COMPLETE && !"BLINDED".equalsIgnoreCase(p.getStatus())) {
                p.changeStatus("BLINDED");
                karmaService.deductKarma(author.getId(), 20, KarmaReason.REPORT_POST_APPROVED, "신고 승인에 따른 게시글 블라인드 (ID: " + targetId + ")", null, true);
                notificationService.send(author, NotificationType.SYSTEM, "⚠️ 작성하신 게시글이 블라인드 처리되었습니다: \"" + p.getTitle() + "\"", targetId, targetId);
            } else if (status == ReportStatus.DELETE_COMPLETE && !"DELETED".equalsIgnoreCase(p.getStatus())) {
                p.changeStatus("DELETED");
                karmaService.deductKarma(author.getId(), 50, KarmaReason.POST_DELETED, "신고 승인에 따른 게시글 영구 삭제 (ID: " + targetId + ")", null, true);
                notificationService.send(author, NotificationType.SYSTEM, "🚫 작성하신 게시글이 운영 정책 위반으로 영구 삭제되었습니다: \"" + p.getTitle() + "\"", null, null);
            } else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                p.changeStatus("ACTIVE");
                if (oldStatus == ReportStatus.BLIND_COMPLETE) karmaService.manualAdjustKarma(author.getId(), 20, KarmaReason.ADMIN_REVERSAL, "조치 번복에 따른 점수 복구", null, false);
                else if (oldStatus == ReportStatus.DELETE_COMPLETE) karmaService.manualAdjustKarma(author.getId(), 50, KarmaReason.ADMIN_REVERSAL, "조치 번복에 따른 점수 복구", null, false);
            }
            notifyReporters(targetId, ReportTargetType.POST, status != ReportStatus.REJECTED && status != ReportStatus.WAITING);
        });
    }

    private void handleCommentSanction(Long targetId, ReportStatus status, ReportStatus oldStatus) {
        syncReportStatus(targetId, ReportTargetType.COMMENT, status);
        commentRepository.findById(targetId).ifPresent(c -> {
            User author = c.getAuthor();
            String preview = c.getContent().length() > 25 ? c.getContent().substring(0, 25) + "..." : c.getContent();
            if (status == ReportStatus.BLIND_COMPLETE && !"BLINDED".equalsIgnoreCase(c.getStatus())) {
                c.changeStatus("BLINDED");
                karmaService.deductKarma(author.getId(), 10, KarmaReason.REPORT_COMMENT_APPROVED, "신고 승인에 따른 댓글 블라인드 (ID: " + targetId + ")", null, true);
                notificationService.send(author, NotificationType.SYSTEM, "⚠️ 작성하신 댓글이 블라인드 처리되었습니다: \"" + preview + "\"", c.getId(), c.getPost().getId());
            } else if (status == ReportStatus.DELETE_COMPLETE && !"DELETED".equalsIgnoreCase(c.getStatus())) {
                c.changeStatus("DELETED");
                karmaService.deductKarma(author.getId(), 30, KarmaReason.COMMENT_DELETED, "신고 승인에 따른 댓글 영구 삭제 (ID: " + targetId + ")", null, true);
                notificationService.send(author, NotificationType.SYSTEM, "🚫 작성하신 댓글이 운영 정책 위반으로 영구 삭제되었습니다: \"" + preview + "\"", null, null);
            } else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                c.changeStatus("ACTIVE");
                if (oldStatus == ReportStatus.BLIND_COMPLETE) karmaService.manualAdjustKarma(author.getId(), 10, KarmaReason.ADMIN_REVERSAL, "조치 번복에 따른 점수 복구", null, false);
                else if (oldStatus == ReportStatus.DELETE_COMPLETE) karmaService.manualAdjustKarma(author.getId(), 30, KarmaReason.ADMIN_REVERSAL, "조치 번복에 따른 점수 복구", null, false);
            }
            notifyReporters(targetId, ReportTargetType.COMMENT, status != ReportStatus.REJECTED && status != ReportStatus.WAITING);
        });
    }

    private void handleMessageSanction(Long targetId, ReportStatus status, ReportStatus oldStatus) {
        syncReportStatus(targetId, ReportTargetType.MESSAGE, status);
        messageService.clearSystemMessages(targetId);
        messageRepository.findById(targetId).ifPresent(m -> {
            User suspect = m.getSender();
            String violation = "운영원칙 위반";
            if (m.getAiAnalysis() != null && !m.getAiAnalysis().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(m.getAiAnalysis());
                    String p = node.path("violationPoint").asText();
                    if (p != null && !p.isEmpty()) violation = p;
                } catch (Exception e) {}
            }
            String preview = m.getContent().length() > 25 ? m.getContent().substring(0, 25) + "..." : m.getContent();

            if (status == ReportStatus.BLIND_COMPLETE) {
                karmaService.deductKarma(suspect.getId(), 30, KarmaReason.REPORT_MESSAGE_APPROVED, "신고 승인에 따른 제재 (ID: " + targetId + ")", null, true);
                notificationService.send(suspect, NotificationType.SYSTEM, "⚠️ 보내신 쪽지가 운영 정책 위반('" + violation + "')으로 블라인드 처리되었습니다: \"" + preview + "\"", suspect.getId(), null);
                messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), "⚠️ [경고] 운영 정책 위반('" + violation + "') 표현 사용으로 제재가 적용되었습니다.", suspect.getId(), false, targetId);
            } else if (status == ReportStatus.DELETE_COMPLETE) {
                karmaService.deductKarma(suspect.getId(), 70, KarmaReason.REPORT_MESSAGE_APPROVED, "심각한 위반으로 인한 영구 제한 (ID: " + targetId + ")", null, true);
                notificationService.send(suspect, NotificationType.SYSTEM, "🚫 보내신 쪽지가 심각한 운영 정책 위반으로 영구 제한되었습니다: \"" + preview + "\"", null, null);
                messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), "🚫 [주의] 심각한 위반('" + violation + "') 정황이 확인되어 본 대화방 이용이 영구 제한됩니다.", suspect.getId(), true, targetId);
            } else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                messageService.restoreMessage(targetId);
                if (oldStatus == ReportStatus.BLIND_COMPLETE) karmaService.manualAdjustKarma(suspect.getId(), 30, KarmaReason.ADMIN_REVERSAL, "조치 번복 점수 복구", null, false);
                else if (oldStatus == ReportStatus.DELETE_COMPLETE) karmaService.manualAdjustKarma(suspect.getId(), 70, KarmaReason.ADMIN_REVERSAL, "조치 번복 점수 복구", null, false);
            }
            notifyReporters(targetId, ReportTargetType.MESSAGE, status != ReportStatus.REJECTED && status != ReportStatus.WAITING);
        });
    }

    private void notifyReporters(Long targetId, ReportTargetType type, boolean isApproved) {
        List<Report> reports = reportRepository.findAllByTargetIdAndTargetType(targetId, type);
        String targetName = type == ReportTargetType.POST ? "게시글" : (type == ReportTargetType.COMMENT ? "댓글" : "쪽지");
        String resultMsg = isApproved ? "운영 정책에 따른 조치가 완료되었습니다." : "위반 사항이 확인되지 않아 반려되었습니다.";
        reports.stream().map(Report::getReporter).distinct().forEach(reporter -> {
            notificationService.send(reporter, NotificationType.SYSTEM, "📢 신고하신 " + targetName + "에 대한 검토 결과, " + resultMsg, targetId, null);
        });
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
