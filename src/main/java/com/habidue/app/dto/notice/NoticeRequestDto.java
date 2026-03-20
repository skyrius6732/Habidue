package com.habidue.app.dto.notice;

import com.habidue.app.domain.notice.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.habidue.app.domain.notice.NoticeStatus;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String content;

    @NotNull(message = "마감일은 필수입니다.")
    private LocalDateTime deadline;

    @NotBlank(message = "링크는 필수입니다.")
    private String link;

    @NotBlank(message = "출처는 필수입니다.")
    private String source;

    private NoticeStatus status;

    public Notice toEntity() {
        return Notice.builder()
                .title(title)
                .content(content)
                .deadline(deadline)
                .link(link)
                .source(source)
                .status(status != null ? status : NoticeStatus.INFO)
                .build();
    }
}
