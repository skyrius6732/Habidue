package com.habidue.app.controller.board;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.dto.search.SearchResponseDto;
import com.habidue.app.dto.tag.TagResponseDto;
import com.habidue.app.service.board.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    /**
     * 통합 검색 API
     * 모든 게시판(PostType)을 아우르는 검색 결과를 반환함
     */
    @GetMapping
    public ResponseEntity<SearchResponseDto> searchAll(
            @RequestParam String keyword,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        return ResponseEntity.ok(postService.searchAll(keyword, pageable, currentUser));
    }

    /**
     * [시니어 조치] 인기 검색어 추천 API
     */
    @GetMapping("/popular-keywords")
    public ResponseEntity<List<TagResponseDto>> getPopularKeywords() {
        return ResponseEntity.ok(postService.getPopularKeywords(5));
    }
}
