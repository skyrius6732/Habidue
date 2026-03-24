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

    /**
     * [시니어 조치] 통합 검색 API
     * 기존 board 패키지의 컨트롤러를 고도화하여 빈 충돌 해결 및 기능 강화
     */
    @GetMapping
    public ResponseEntity<ApiResponse<IntegratedSearchResponseDto>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Pageable pageable = PageRequest.of(page, size);
        
        // 1. 전체 검색 결과 (통합)
        Page<PostResponseDto> totalPage = postService.getPosts(null, null, null, null, keyword, null, null, currentUser, pageable);
        
        // 2. 카테고리별 건수 집계
        Map<String, Long> categoryCounts = new HashMap<>();
        for (PostType type : PostType.values()) {
            long count = postService.countByKeywordAndType(keyword, type);
            categoryCounts.put(type.name(), count);
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
    public ResponseEntity<ApiResponse<List<String>>> getPopularKeywords() {
        return ApiResponse.success(List.of("LH공고", "SH공고", "청년주택", "당첨후기", "이사꿀팁", "인테리어"));
    }
}
