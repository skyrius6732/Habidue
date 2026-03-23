package com.habidue.app.service.board;

import com.habidue.app.domain.board.*;
import com.habidue.app.domain.user.KarmaReason;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.admin.ConversationReportGroupDto;
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.board.ReportRepository;
import com.habidue.app.service.message.MessageService;
import com.habidue.app.service.user.KarmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final com.habidue.app.repository.message.MessageRepository messageRepository;
    private final ReportRepository reportRepository;
    private final KarmaService karmaService;
    private final MessageService messageService;

    public Page<PostResponseDto> getAdminPosts(Long userId, String keyword, String status, Pageable pageable) {
        if (userId != null) return postRepository.findPostsByAuthor(userId, keyword, status, pageable).map(PostResponseDto::from);
        return postRepository.findPosts(null, null, null, null, keyword, status, null, null, pageable, null, true).map(PostResponseDto::from);
    }

    @Transactional
    public void changePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        String oldStatus = post.getStatus();
        post.changeStatus(status);
        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(post.getAuthor().getId(), 20, KarmaReason.REPORT_APPROVED, "관리자에 의한 게시글 블라인드 (ID: " + postId + ")", null, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(postId, ReportTargetType.POST, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(post.getAuthor().getId(), 50, KarmaReason.POST_DELETED, "심각한 운영원칙 위반으로 인한 게시글 삭제 (ID: " + postId + ")", null, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) syncReportStatus(postId, ReportTargetType.POST, ReportStatus.REJECTED);
    }

    @Transactional
    public void updatePost(Long postId, String title, String content) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        post.update(title, content, post.getCategory(), post.getSubCategory(), post.getRegionTag());
    }

    public Page<CommentResponseDto> getAdminComments(Long userId, String keyword, String status, Pageable pageable) {
        return commentRepository.findComments(userId, keyword, status, pageable).map(comment -> CommentResponseDto.from(comment, true));
    }

    @Transactional
    public void changeCommentStatus(Long commentId, String status) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        String oldStatus = comment.getStatus();
        comment.changeStatus(status);
        if ("BLINDED".equalsIgnoreCase(status) && !"BLINDED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(comment.getAuthor().getId(), 10, KarmaReason.REPORT_APPROVED, "관리자에 의한 댓글 블라인드 (ID: " + commentId + ")", null, true);
        } else if ("DELETED".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(oldStatus)) {
            syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.BLIND_COMPLETE);
            karmaService.deductKarma(comment.getAuthor().getId(), 30, KarmaReason.COMMENT_DELETED, "심각한 운영원칙 위반으로 인한 댓글 삭제 (ID: " + commentId + ")", null, true);
        } else if ("ACTIVE".equalsIgnoreCase(status)) syncReportStatus(commentId, ReportTargetType.COMMENT, ReportStatus.REJECTED);
    }

    @Transactional
    public void updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        comment.updateContent(content);
    }

    public Page<ReportAdminResponseDto> getAdminReports(ReportTargetType targetType, String status, String keyword, Pageable pageable) {
        return reportRepository.findGroupedReports(targetType, status, keyword, pageable);
    }

    public List<ConversationReportGroupDto> getGroupedMessageReports(String status, String keyword) {
        // [시니어 조치] 최신순으로 정렬된 신고 리스트를 가져옴
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

            ConversationReportGroupDto group = groupedMap.getOrDefault(convKey, ConversationReportGroupDto.builder()
                    .conversationId(convKey)
                    .build());

            if (group.getUser1Name() == null) {
                User u1 = sender.getId().equals(u1Id) ? sender : receiver;
                User u2 = sender.getId().equals(u2Id) ? sender : receiver;
                group.setUser1Name(u1.getNickname());
                group.setUser2Name(u2.getNickname());
            }

            Long reporterId = report.getReporters().get(0).getReporterId();
            
            // [시니어 조치] 이미 해당 슬롯에 신고가 있더라도, 현재 가져온 데이터가 더 최신이거나 WAITING 상태라면 우선시함
            if (reporterId.equals(u1Id)) {
                if (group.getUser1Report() == null || group.getUser1Report().getReportStatus().equals("REJECTED")) {
                    group.setUser1Report(report);
                }
            } else if (reporterId.equals(u2Id)) {
                if (group.getUser2Report() == null || group.getUser2Report().getReportStatus().equals("REJECTED")) {
                    group.setUser2Report(report);
                }
            }
            
            groupedMap.put(convKey, group);
        }

        return new java.util.ArrayList<>(groupedMap.values());
    }

    @Transactional
    public void handleReport(Long targetId, ReportTargetType targetType, ReportStatus status) {
        // [시니어 조치] 상태 변경 전, 현재 신고들의 통합 상태를 확인하여 복구 로직 필요 여부 판단
        List<Report> existingReports = reportRepository.findAllByTargetIdAndTargetType(targetId, targetType);
        ReportStatus oldStatus = existingReports.isEmpty() ? ReportStatus.WAITING : existingReports.get(0).getStatus();

        syncReportStatus(targetId, targetType, status);

        if (targetType == ReportTargetType.POST) {
            postRepository.findById(targetId).ifPresent(p -> {
                String postOldStatus = p.getStatus();
                if (status == ReportStatus.BLIND_COMPLETE && !"BLINDED".equalsIgnoreCase(postOldStatus)) {
                    p.changeStatus("BLINDED");
                    karmaService.deductKarma(p.getAuthor().getId(), 20, KarmaReason.REPORT_POST_APPROVED, "신고 승인에 따른 게시글 블라인드 (ID: " + targetId + ")", null, true);
                } else if (status == ReportStatus.DELETE_COMPLETE && !"DELETED".equalsIgnoreCase(postOldStatus)) {
                    p.changeStatus("DELETED");
                    karmaService.deductKarma(p.getAuthor().getId(), 50, KarmaReason.POST_DELETED, "신고 승인에 따른 게시글 영구 삭제 (ID: " + targetId + ")", null, true);
                } else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                    p.changeStatus("ACTIVE");
                    // [시니어 조치] 번복 시 점수 복구
                    if (oldStatus == ReportStatus.BLIND_COMPLETE) {
                        karmaService.manualAdjustKarma(p.getAuthor().getId(), 20, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (게시글 ID: " + targetId + ")", null, false);
                    } else if (oldStatus == ReportStatus.DELETE_COMPLETE) {
                        karmaService.manualAdjustKarma(p.getAuthor().getId(), 50, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (게시글 ID: " + targetId + ")", null, false);
                    }
                }
            });
        } else if (targetType == ReportTargetType.COMMENT) {
            commentRepository.findById(targetId).ifPresent(c -> {
                String commentOldStatus = c.getStatus();
                if (status == ReportStatus.BLIND_COMPLETE && !"BLINDED".equalsIgnoreCase(commentOldStatus)) {
                    c.changeStatus("BLINDED");
                    karmaService.deductKarma(c.getAuthor().getId(), 10, KarmaReason.REPORT_COMMENT_APPROVED, "신고 승인에 따른 댓글 블라인드 (ID: " + targetId + ")", null, true);
                } else if (status == ReportStatus.DELETE_COMPLETE && !"DELETED".equalsIgnoreCase(commentOldStatus)) {
                    c.changeStatus("DELETED");
                    karmaService.deductKarma(c.getAuthor().getId(), 30, KarmaReason.COMMENT_DELETED, "신고 승인에 따른 댓글 영구 삭제 (ID: " + targetId + ")", null, true);
                } else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                    c.changeStatus("ACTIVE");
                    // [시니어 조치] 번복 시 점수 복구
                    if (oldStatus == ReportStatus.BLIND_COMPLETE) {
                        karmaService.manualAdjustKarma(c.getAuthor().getId(), 10, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (댓글 ID: " + targetId + ")", null, false);
                    } else if (oldStatus == ReportStatus.DELETE_COMPLETE) {
                        karmaService.manualAdjustKarma(c.getAuthor().getId(), 30, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (댓글 ID: " + targetId + ")", null, false);
                    }
                }
            });
        } else if (targetType == ReportTargetType.MESSAGE) {
            messageService.clearSystemMessages(targetId);
            messageRepository.findById(targetId).ifPresent(m -> {
                User reporter = findReporterForTarget(targetId, ReportTargetType.MESSAGE);
                User suspect = m.getSender();

                // 1. 신고 승인 (블라인드 / 1단계 / 주황색)
                if (status == ReportStatus.BLIND_COMPLETE) {
                    karmaService.deductKarma(suspect.getId(), 30, KarmaReason.REPORT_MESSAGE_APPROVED, "신고 승인에 따른 제재 (대상 ID: " + targetId + ")", null, true);
                    String violation = "운영원칙 위반";
                    if (m.getAiAnalysis() != null && !m.getAiAnalysis().isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(m.getAiAnalysis());
                            String p = node.path("violationPoint").asText();
                            if (p != null && !p.isEmpty()) violation = p;
                        } catch (Exception e) {}
                    }
                    // [시니어 조치] 블라인드 시에는 경고만 주고 방은 폭파하지 않음 (isRoomRestricted = false)
                    String suspectMsg = String.format("⚠️ [경고] %s님, 회원님께서 사용하신 표현은 '%s' 정황이 확인되어 운영 정책에 따른 제재(신뢰점수 감점)가 적용되었습니다. 이후에도 부적절한 언행이 반복될 경우 서비스 이용이 제한될 수 있으니 매너 있는 대화를 부탁드립니다.", suspect.getNickname(), violation);
                    String reporterMsg = String.format("📢 [안내] 신고하신 내용에 대해 관리자 검토가 완료되었습니다. 해당 메시지는 '%s' 사유로 대상자에게는 운영 정책에 따른 제재가 적용되었습니다.", violation);
                    if (reporter != null) messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), reporterMsg, reporter.getId(), false, targetId);
                    messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), suspectMsg, suspect.getId(), false, targetId);
                } 
                else if (status == ReportStatus.REJECTED || status == ReportStatus.WAITING) {
                    messageService.restoreMessage(targetId);
                    if (status == ReportStatus.REJECTED && reporter != null) {
                        messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), "📢 [안내] 신고하신 내용을 검토했으나 위반 사항이 확인되지 않아 반려되었습니다.", reporter.getId(), false, targetId);
                    }
                    // [시니어 조치] 번복 시 점수 복구
                    if (oldStatus == ReportStatus.BLIND_COMPLETE) {
                        karmaService.manualAdjustKarma(suspect.getId(), 30, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (쪽지 ID: " + targetId + ")", null, false);
                    } else if (oldStatus == ReportStatus.DELETE_COMPLETE) {
                        karmaService.manualAdjustKarma(suspect.getId(), 70, KarmaReason.ADMIN_REVERSAL, "관리자 조치 번복에 따른 점수 복구 (쪽지 ID: " + targetId + ")", null, false);
                    }
                }
                // 4. 영구 제재 (2단계 / 빨간색)
                else if (status == ReportStatus.DELETE_COMPLETE) {
                    karmaService.deductKarma(suspect.getId(), 70, KarmaReason.REPORT_MESSAGE_APPROVED, "심각한 위반으로 인한 영구 제한 (대상 ID: " + targetId + ")", null, true);
                    String violation = "심각한 운영원칙 위반";
                    if (m.getAiAnalysis() != null && !m.getAiAnalysis().isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(m.getAiAnalysis());
                            String p = node.path("violationPoint").asText();
                            if (p != null && !p.isEmpty()) violation = p;
                        } catch (Exception e) {}
                    }
                    // [시니어 조치] 영구 삭제 시에만 방을 폭파함 (isRoomRestricted = true)
                    String suspectMsg = String.format("🚫 [주의] %s님, 회원님께서 사용하신 표현은 '%s' 정황이 확인되어 본 대화방에서의 메시지 발송이 영구적으로 제한되며, 운영 정책에 따른 강력한 제재(신뢰점수 감점 포함)가 적용되었습니다.", suspect.getNickname(), violation);
                    String reporterMsg = String.format("📢 [안내] 신고하신 내용에 대해 관리자 검토가 완료되었습니다. 심각한 '%s' 정황이 확인되어 해당 대화방 영구 제한 및 대상자 차단/제재 조치가 완료되었습니다.", violation);
                    if (reporter != null) { 
                        messageService.blockUser(reporter, suspect.getId(), "심각한 운영원칙 위반('" + violation + "')으로 인한 자동 격리", true); 
                        messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), reporterMsg, reporter.getId(), true, targetId); 
                    }
                    messageService.sendAdminResultSystemMessage(m.getSender(), m.getReceiver(), suspectMsg, suspect.getId(), true, targetId);
                }
            });
        }
    }

    private User findReporterForTarget(Long targetId, ReportTargetType type) {
        return reportRepository.findAllByTargetIdAndTargetType(targetId, type).stream().findFirst().map(Report::getReporter).orElse(null);
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
