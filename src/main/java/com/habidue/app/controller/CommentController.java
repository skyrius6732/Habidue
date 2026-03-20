package com.habidue.app.controller;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.board.CommentRequestDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.service.board.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 특정 게시글의 댓글 목록 조회
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponseDto>>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 50) Pageable pageable) {
        
        return ApiResponse.success(commentService.getComments(postId, pageable));
    }

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto requestDto) {
        
        return ApiResponse.success(HttpStatus.CREATED, commentService.createComment(postId, requestDto));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }

    // 댓글 수정
    @PutMapping("/comments/{commentId}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<CommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto requestDto) {
        
        return ApiResponse.success(commentService.updateComment(commentId, requestDto));
    }

    // [시니어] 댓글 좋아요/취소 토글
    @PostMapping("/comments/{commentId}/like")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> toggleCommentLike(@PathVariable Long commentId) {
        commentService.toggleCommentLike(commentId);
        return ApiResponse.success(null);
    }
}
