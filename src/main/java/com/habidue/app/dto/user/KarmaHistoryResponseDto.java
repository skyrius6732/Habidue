package com.habidue.app.dto.user;

import com.habidue.app.domain.user.KarmaHistory;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class KarmaHistoryResponseDto {
    private Long id;
    private String reason; // Enum name
    private String reasonDescription; // 한글 설명 (예: "관리자 수동 조정")
    private int pointChange;
    private int resultingPoint;
    private String comment;
    private String adminName;
    private LocalDateTime createdAt;

    public KarmaHistoryResponseDto(KarmaHistory history) {
        this.id = history.getId();
        this.reason = history.getReason().name();
        this.reasonDescription = history.getReason().getDescription(); // 여기서 한글 설명 매핑
        this.pointChange = history.getPointChange();
        this.resultingPoint = history.getResultingPoint();
        this.comment = history.getComment();
        this.adminName = history.getAdmin() != null ? history.getAdmin().getUsername() : "SYSTEM";
        this.createdAt = history.getCreatedAt();
    }
}
