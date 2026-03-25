package com.habidue.app.controller.board;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.board.IntegratedSearchResponseDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.service.board.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;
    private final com.habidue.app.service.tag.TagService tagService;

    /**
     * [시니어 조치] 통합 검색 API
     * 기존 board 패키지의 컨트롤러를 고도화하여 빈 충돌 해결 및 기능 강화
     */
    @GetMapping
    public ResponseEntity<ApiResponse<IntegratedSearchResponseDto>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) PostType type, // [시니어 조치] 카테고리 필터 파라미터 추가
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        
        // 1. 검색 결과 조회 (type이 null이면 전체, 값이 있으면 해당 카테고리만)
        Page<PostResponseDto> totalPage = postService.getPosts(type, null, null, null, keyword, null, null, currentUser, pageable);
        
        // 2. 카테고리별 건수 집계
        Map<String, Long> categoryCounts = new HashMap<>();
        for (PostType postType : PostType.values()) {
            long count = postService.countByKeywordAndType(keyword, postType, currentUser);
            categoryCounts.put(postType.name(), count);
        }

        // 3. 결과 변환
        List<IntegratedSearchResponseDto.SearchResultItemDto> items = totalPage.getContent().stream()
                .map(post -> IntegratedSearchResponseDto.SearchResultItemDto.builder()
                        .post(post)
                        .snippet(post.getContent().length() > 100 ? post.getContent().substring(0, 100) + "..." : post.getContent())
                        .build())
                .collect(Collectors.toList());

        IntegratedSearchResponseDto response = IntegratedSearchResponseDto.builder()
                .totalCount(totalPage.getTotalElements())
                .categoryCounts(categoryCounts)
                .results(items)
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/popular-keywords")
    public ResponseEntity<ApiResponse<List<com.habidue.app.dto.tag.TagResponseDto>>> getPopularKeywords() {
        // [시니어 조치] 하드코딩 제거 및 TagService를 통한 실제 인기 태그 반환
        return ApiResponse.success(tagService.getPopularTags(10));
    }
}
