package com.habidue.app.dto.admin;

import com.habidue.app.domain.board.ReportStatus;
import com.habidue.app.domain.board.ReportTargetType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportAdminResponseDto {
    private Long targetId;
    private ReportTargetType targetType;
    private String targetTitle;   
    private String authorName;    
    private Long reportCount;     
    private String latestReason;  
    private LocalDateTime latestReportDate; 
    private String targetStatus;  
    private ReportStatus reportStatus; 
    private Long parentPostId; // [시니어 조치] 댓글일 경우 이동할 게시글 ID

    // [시니어 조치] 신고자 상세 정보 리스트 추가
    private List<ReporterDetail> reporters;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReporterDetail {
        private Long reporterId;
        private String reporterName;
        private int reporterKarma;
        private String reason;
        private LocalDateTime reportedAt;
    }
}
