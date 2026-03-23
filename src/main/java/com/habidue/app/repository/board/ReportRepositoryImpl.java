package com.habidue.app.repository.board;

import com.habidue.app.domain.board.*;
import com.habidue.app.domain.message.QMessage; // 추가
import com.habidue.app.dto.admin.ReportAdminResponseDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ReportAdminResponseDto> findGroupedReports(ReportTargetType targetType, String status, String keyword, Pageable pageable) {
        QReport report = QReport.report;
        QPost post = QPost.post;
        QComment comment = QComment.comment;

        BooleanBuilder builder = new BooleanBuilder();

        if (targetType != null) {
            builder.and(report.targetType.eq(targetType));
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            builder.and(report.status.eq(ReportStatus.valueOf(status)));
        }

        // [시니어 조치] 신고 사유 키워드 검색 추가
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(report.reason.containsIgnoreCase(keyword));
        }

        com.querydsl.jpa.impl.JPAQuery<Long> query = queryFactory
                .select(report.id.max())
                .from(report)
                .where(builder)
                .groupBy(report.targetId, report.targetType)
                .orderBy(report.id.max().desc());

        if (pageable.isPaged()) {
            query.offset(pageable.getOffset())
                 .limit(pageable.getPageSize());
        }

        List<Long> latestReportIds = query.fetch();

        if (latestReportIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Report> reports = queryFactory
                .selectFrom(report)
                .where(report.id.in(latestReportIds))
                .orderBy(report.createdAt.desc())
                .fetch();

        List<ReportAdminResponseDto> content = reports.stream().map(r -> {
            ReportAdminResponseDto dto = ReportAdminResponseDto.builder()
                    .targetId(r.getTargetId())
                    .targetType(r.getTargetType())
                    .latestReason(r.getReason())
                    .latestReportDate(r.getCreatedAt())
                    .reportStatus(r.getStatus())
                    .build();

            // [시니어 조치] 신고자 리스트 수집 (카르마 점수 포함)
            List<Report> allReportsForTarget = queryFactory
                    .selectFrom(report)
                    .leftJoin(report.reporter).fetchJoin()
                    .where(report.targetId.eq(r.getTargetId()).and(report.targetType.eq(r.getTargetType())))
                    .orderBy(report.createdAt.desc())
                    .fetch();

            dto.setReporters(allReportsForTarget.stream().map(rep -> 
                ReportAdminResponseDto.ReporterDetail.builder()
                        .reporterId(rep.getReporter().getId())
                        .reporterName(rep.getReporter().getNickname() != null ? rep.getReporter().getNickname() : rep.getReporter().getUsername())
                        .reporterKarma(rep.getReporter().getKarmaPoint())
                        .reason(rep.getReason())
                        .reportedAt(rep.getCreatedAt())
                        .build()
            ).collect(Collectors.toList()));
            dto.setReportCount((long) allReportsForTarget.size());

            // 대상 상세 정보 (게시글/댓글/쪽지 분기 처리)
            if (r.getTargetType() == ReportTargetType.POST) {
                Post p = queryFactory.selectFrom(post).where(post.id.eq(r.getTargetId())).fetchOne();
                if (p != null) {
                    dto.setTargetTitle(p.getTitle());
                    dto.setAuthorName(p.getAuthor().getNickname() != null ? p.getAuthor().getNickname() : p.getAuthor().getUsername());
                    dto.setTargetStatus(p.getStatus());
                    dto.setParentPostId(p.getId());
                }
            } else if (r.getTargetType() == ReportTargetType.COMMENT) {
                Comment c = queryFactory.selectFrom(comment).where(comment.id.eq(r.getTargetId())).fetchOne();
                if (c != null) {
                    dto.setTargetTitle(c.getContent());
                    dto.setAuthorName(c.getAuthor().getNickname() != null ? c.getAuthor().getNickname() : c.getAuthor().getUsername());
                    dto.setTargetStatus(c.getStatus());
                    dto.setParentPostId(c.getPost().getId());
                }
            } else if (r.getTargetType() == ReportTargetType.MESSAGE) {
                // [시니어 조치] MESSAGE 타입 매핑 추가
                QMessage message = QMessage.message;
                com.habidue.app.domain.message.Message m = queryFactory.selectFrom(message).where(message.id.eq(r.getTargetId())).fetchOne();
                if (m != null) {
                    dto.setTargetTitle(m.getContent());
                    dto.setAuthorName(m.getSender().getNickname() != null ? m.getSender().getNickname() : m.getSender().getUsername());
                    dto.setTargetStatus(m.isDeleted() ? "DELETED" : "ACTIVE");
                    dto.setParentPostId(null);
                }
            }

            return dto;
        }).collect(Collectors.toList());

        long total = queryFactory
                .select(report.targetId, report.targetType)
                .from(report)
                .where(builder)
                .groupBy(report.targetId, report.targetType)
                .fetch().size();

        return new PageImpl<>(content, pageable, total);
    }
}
