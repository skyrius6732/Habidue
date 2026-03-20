package com.habidue.app.dto.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotNull(message = "게시글 타입을 선택해주세요.")
    private PostType type;

    private String category; // 소통 주제 (Flair: 동네소식, 당첨비결 등)

    private String subCategory; // 소메뉴 위치 (게시판 위치: 서울, 경기, 인터뷰 등)

    private String regionTag; // 활동 지역 (전시용 대표 태그)

    private List<Long> tagIds; // 선택된 태그 ID 리스트

    private List<String> imageUrls; // 이미지 URL 목록

    private Long noticeId; // 공고 전용 게시판일 경우 필수
}
