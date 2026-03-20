package com.habidue.app.dto.search;

import com.habidue.app.dto.board.PostResponseDto;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 통합 검색 결과 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponseDto {
    private String keyword;
    private long totalCount;
    private Map<String, Long> categoryCounts; // [시니어 조치] 카테고리별 실제 전체 개수
    
    // 카테고리별 검색 결과 (예: "NOTICE_COMMUNICATION", "GENERAL_COMMUNITY" 등)
    private Map<String, List<SearchResultItem>> resultsByCategory;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResultItem {
        private PostResponseDto post;
        private String snippet; // 검색어가 포함된 본문 일부
        private double searchScore; // 검색 가중치 점수
    }
}
