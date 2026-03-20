package com.habidue.app.dto.board;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequestDto {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    private Long parentId; // 대댓글일 경우 필수
}
