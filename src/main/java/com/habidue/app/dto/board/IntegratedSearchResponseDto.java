package com.habidue.app.dto.board;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegratedSearchResponseDto {
    private long totalCount;
    private Map<String, Long> categoryCounts; // 카테고리(PostType)별 검색 건수
    private List<SearchResultItemDto> results; // 통합 검색 결과 (최신순)

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResultItemDto {
        private PostResponseDto post;
        private String snippet; // 본문 앞부분 일부 (검색어 강조용)
    }
}
