package com.habidue.app.dto.usernotice;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNoticeRequestDto {

    @NotNull(message = "공고 ID는 필수입니다.")
    private Long noticeId;

    private String memo; // 공고에 대한 메모 (선택 사항)

    private LocalDateTime userDeadline; // 추가
}
