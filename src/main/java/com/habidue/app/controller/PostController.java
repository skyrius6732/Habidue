package com.habidue.app.controller;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.board.PostRequestDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.service.board.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시글 목록 조회 (통합 광장, 공고 전용, 후기 등 필터 지원)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPosts(
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) Long noticeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String regionTag,
            @RequestParam(required = false) String tagName,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        
        return ApiResponse.success(postService.getPosts(type, noticeId, category, subCategory, keyword, regionTag, tagName, currentUser, pageable));
    }

    // [공고 상세 미리보기 전용] 인기 게시글 TOP 3 조회
    @GetMapping("/notice/{noticeId}/top")
    public ResponseEntity<ApiResponse<java.util.List<PostResponseDto>>> getTopPosts(
            @PathVariable Long noticeId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(postService.getTopPosts(noticeId, limit));
    }

    // [사이드바 전용] 전체 실시간 인기글 조회
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<java.util.List<PostResponseDto>>> getTrendingPosts(
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(postService.getTrendingPosts(limit));
    }

    // 게시글 상세 조회 (조회수 증가 포함)
    @GetMapping("/{postId:[0-9]+}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(postService.getPostDetail(postId, currentUser));
    }

    // 게시글 좋아요 토글
    @PostMapping("/{postId:[0-9]+}/like")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(postService.toggleLike(postId, currentUser));
    }

    // 게시글 생성
    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @Valid @RequestBody PostRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(postService.createPost(requestDto, currentUser));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId:[0-9]+}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        postService.deletePost(postId, currentUser);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }

    // 게시글 수정
    @PutMapping("/{postId:[0-9]+}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(postService.updatePost(postId, requestDto, currentUser));
    }
}
