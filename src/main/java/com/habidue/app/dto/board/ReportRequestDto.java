package com.habidue.app.dto.board;

import com.habidue.app.domain.board.ReportTargetType;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDto {
    @NotNull(message = "신고 대상을 지정해주세요.")
    private ReportTargetType targetType;

    @NotNull(message = "신고 대상 ID가 필요합니다.")
    private Long targetId;

    @NotBlank(message = "신고 사유를 입력해주세요.")
    private String reason;
}
