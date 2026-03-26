package com.habidue.app.controller.admin;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.dto.admin.SimulationResponseDto;
import com.habidue.app.dto.admin.SimulationUserStatsDto;
import com.habidue.app.dto.board.CommentRequestDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.dto.board.PostRequestDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.repository.user.ExpHistoryRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.board.CommentService;
import com.habidue.app.service.board.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/admin/simulation")
@RequiredArgsConstructor
public class AdminSimulationController {

    private final UserRepository userRepository;
    private final UserActivityStatsRepository statsRepository;
    private final ExpHistoryRepository expHistoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final PostService postService;
    private final CommentService commentService;

    private final AtomicInteger postCounter = new AtomicInteger(1);
    private final AtomicInteger commentCounter = new AtomicInteger(1);

    @GetMapping("/user/{userId}")
    public ResponseEntity<SimulationUserStatsDto> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(getStatsDto(userId));
    }

    /**
     * 게시글 상세 정보 (좋아요 상태 포함) 조회
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponseDto> getPostDetail(@PathVariable Long postId, @RequestParam Long performerId) {
        setPerformerContext(performerId);
        try {
            return ResponseEntity.ok(postService.getPostDetail(postId, (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 특정 게시글의 댓글/답글 목록 조회 (좋아요 상태 포함)
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponseDto>> getPostComments(@PathVariable Long postId, @RequestParam Long performerId) {
        setPerformerContext(performerId);
        try {
            // commentService.getComments는 내부적으로 SecurityContext의 유저를 기준으로 isLiked를 설정함
            return ResponseEntity.ok(commentService.getComments(postId, PageRequest.of(0, 100)).getContent());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void setPerformerContext(Long performerId) {
        User performer = userRepository.findById(performerId).orElseThrow();
        UserPrincipal principal = UserPrincipal.create(performer);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private SimulationResponseDto wrapSimulation(Long subjectId, Long actorId, Long performerId, String actionName, Runnable action) {
        SimulationUserStatsDto subjectBefore = getStatsDto(subjectId);
        SimulationUserStatsDto actorBefore = getStatsDto(actorId);

        setPerformerContext(performerId);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }

        return SimulationResponseDto.builder()
                .actionName(actionName)
                .subjectBefore(subjectBefore)
                .subjectAfter(getStatsDto(subjectId))
                .actorBefore(actorBefore)
                .actorAfter(getStatsDto(actorId))
                .build();
    }

    private SimulationUserStatsDto getStatsDto(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        UserActivityStats stats = statsRepository.findById(userId).orElseThrow();
        List<com.habidue.app.domain.user.ExpHistory> history = expHistoryRepository.findByUserId(userId);
        return SimulationUserStatsDto.from(user, stats, history);
    }

    @PostMapping("/post")
    public ResponseEntity<SimulationResponseDto> simulatePost(
            @RequestParam Long subjectId, @RequestParam Long actorId, @RequestParam Long performerId,
            @RequestParam PostType type, @RequestParam String category, @RequestParam boolean hasPhoto) {
        
        final List<Long> createdId = new ArrayList<>();
        SimulationResponseDto response = wrapSimulation(subjectId, actorId, performerId, "게시글 작성", () -> {
            PostRequestDto dto = new PostRequestDto();
            dto.setTitle(String.format("[%d] 시뮬레이션 테스트글", postCounter.getAndIncrement()));
            dto.setContent("시뮬레이션 본문 내용입니다.");
            dto.setType(type);
            dto.setCategory(category);
            if (type == PostType.NOTICE) dto.setNoticeId(noticeRepository.findAll().get(0).getId());
            if (hasPhoto) dto.setImageUrls(List.of("https://picsum.photos/200"));
            createdId.add(postService.createPost(dto, (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId());
        });
        response.setTargetId(createdId.isEmpty() ? null : createdId.get(0));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comment")
    public ResponseEntity<SimulationResponseDto> simulateComment(
            @RequestParam Long subjectId, @RequestParam Long actorId, @RequestParam Long performerId,
            @RequestParam Long postId, @RequestParam(required = false) Long parentId) {
        
        final List<Long> createdId = new ArrayList<>();
        String label = (parentId == null) ? "댓글" : "답글";
        SimulationResponseDto response = wrapSimulation(subjectId, actorId, performerId, label + " 작성", () -> {
            CommentRequestDto dto = new CommentRequestDto();
            dto.setContent(String.format("[%d] 테스트 %s", commentCounter.getAndIncrement(), label));
            dto.setParentId(parentId);
            createdId.add(commentService.createComment(postId, dto).getId());
        });
        response.setTargetId(createdId.isEmpty() ? null : createdId.get(0));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/like")
    public ResponseEntity<SimulationResponseDto> simulateLike(
            @RequestParam Long subjectId, @RequestParam Long actorId, @RequestParam Long performerId,
            @RequestParam String targetType, @RequestParam Long targetId) {
        
        return ResponseEntity.ok(wrapSimulation(subjectId, actorId, performerId, targetType + " 좋아요", () -> {
            if ("POST".equals(targetType)) {
                postService.toggleLike(targetId, (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            } else {
                commentService.toggleCommentLike(targetId);
            }
        }));
    }

    @PostMapping("/delete-target")
    public ResponseEntity<SimulationResponseDto> simulateDeleteTarget(
            @RequestParam Long subjectId, @RequestParam Long actorId, @RequestParam Long performerId,
            @RequestParam String type, @RequestParam Long targetId) {
        
        return ResponseEntity.ok(wrapSimulation(subjectId, actorId, performerId, type + " 삭제", () -> {
            if ("post".equals(type)) {
                postService.deletePost(targetId, (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            } else {
                commentService.deleteComment(targetId);
            }
        }));
    }
}
