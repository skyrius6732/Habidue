package com.habidue.app.service.board;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.board.Comment;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.dto.board.CommentRequestDto;
import com.habidue.app.dto.board.CommentResponseDto;
import com.habidue.app.repository.board.CommentRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.repository.badge.UserBadgeRepository;
import com.habidue.app.service.badge.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import com.habidue.app.service.user.ExpService;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.service.user.KarmaService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeService badgeService;
    private final com.habidue.app.repository.badge.BadgeLevelRuleRepository badgeLevelRuleRepository;
    private final ExpService expService; // [시니어 조치] EXP 연동
    private final KarmaService karmaService; // [시니어 조치] 카르마 시스템 연동
    private final com.habidue.app.repository.board.CommentLikeRepository commentLikeRepository; // [시니어] 댓글 좋아요 레포지토리 추가

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto) {
        User author = getCurrentUser();

        // [시니어 페널티 체크] 활동 제한 여부 검증
        if (karmaService.isRestricted(author)) {
            java.time.LocalDateTime until = author.getRestrictedUntil();
            String message;
            
            // 50년 이상 제한이면 사실상 영구 제재로 간주
            if (until != null && until.isAfter(java.time.LocalDateTime.now().plusYears(50))) {
                message = "카르마 점수가 너무 낮아 커뮤니티에서 영구 활동이 제한되었습니다.\n상세 내용은 고객센터로 문의해 주세요.";
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                message = "카르마 점수가 낮아 커뮤니티 활동이 제한된 상태입니다.\n(제한 종료 시각: " + until.format(formatter) + ")";
            }
            throw new IllegalArgumentException(message);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        // [시니어 조치] 관리자에 의해 블라인드 처리된 게시글은 댓글 작성 차단
        if ("BLINDED".equalsIgnoreCase(post.getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.");
        }

        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new NoSuchElementException("부모 댓글을 찾을 수 없습니다."));
        }

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .post(post)
                .author(author)
                .parent(parent)
                .build();

        // 1. 댓글수 원자적 증가 (Atomic counting)
        postRepository.incrementCommentCount(postId);

        // [시니어 조치] 유저 활동 통계 업데이트 (지연 초기화 적용)
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        
        stats.incrementCommentCount();
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats); // [시니어 조치] 배지 획득 조건 체크

        Comment savedComment = commentRepository.save(comment);

        // [시니어 조치] 댓글 작성 EXP 부여
        expService.grantExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 작성: " + savedComment.getId());

        return convertToDtoWithBadges(savedComment);
    }

    public Page<CommentResponseDto> getComments(Long postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);
        return comments.map(this::convertToDtoWithBadges);
    }
/**
 * [시니어 조치] 댓글 DTO 변환 시 작성자 배지 정보를 포함함 (재귀 처리 및 관리자 권한 확인)
 */
private CommentResponseDto convertToDtoWithBadges(Comment comment) {
    boolean isAdmin = false;
    Long currentUserId = null;
    try {
        User currentUser = getCurrentUser();
        isAdmin = currentUser.getRole() == com.habidue.app.domain.user.Role.ADMIN;
        currentUserId = currentUser.getId();
    } catch (Exception e) { /* 비로그인 상태 등 */ }

    CommentResponseDto dto = CommentResponseDto.from(comment, isAdmin, currentUserId, commentLikeRepository);

        // 1. 본문 댓글 작성자 배지 주입
        dto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(comment.getAuthor()).stream()
                .map(ub -> {
                    String badgeType = ub.getBadge().getCode().replace("_BASE", "");
                    com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
                    String realName = (rule != null) ? rule.getFullDisplayName() : ub.getBadge().getName();
                    return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, realName);
                })
                .collect(java.util.stream.Collectors.toList()));

        // 2. 대댓글들에 대해서도 재귀적으로 배지 주입
        if (dto.getChildren() != null) {
            for (int i = 0; i < comment.getChildren().size(); i++) {
                Comment childEntity = comment.getChildren().get(i);
                CommentResponseDto childDto = dto.getChildren().get(i);
                
                childDto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(childEntity.getAuthor()).stream()
                        .map(ub -> {
                            String badgeType = ub.getBadge().getCode().replace("_BASE", "");
                            com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
                            String realName = (rule != null) ? rule.getFullDisplayName() : ub.getBadge().getName();
                            return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, realName);
                        })
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        
        return dto;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // [시니어 조치] 관리자에 의해 블라인드 처리된 게시글은 댓글 삭제 차단
        if ("BLINDED".equalsIgnoreCase(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.");
        }

        // [시니어 조치] 관리자에 의해 블라인드 또는 영구 삭제 처리된 댓글은 작성자가 조작할 수 없음
        if ("BLINDED".equals(comment.getStatus()) || "DELETED".equals(comment.getStatus())) {
            throw new IllegalStateException("관리자에 의해 조치된 댓글은 수정 또는 삭제할 수 없습니다.");
        }

        // 1. 댓글수 원자적 감소
        postRepository.decrementCommentCount(comment.getPost().getId());
        
        commentRepository.delete(comment);

        // [시니어 조치] 유저 활동 통계 업데이트 (댓글 삭제)
        userActivityStatsRepository.findById(currentUser.getId()).ifPresent(stats -> {
            stats.decrementCommentCount();
            userActivityStatsRepository.save(stats);
        });
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        // [시니어 조치] 관리자에 의해 블라인드 처리된 게시글은 댓글 수정 차단
        if ("BLINDED".equalsIgnoreCase(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.");
        }

        // [시니어 조치] 관리자에 의해 블라인드 또는 영구 삭제 처리된 댓글은 작성자가 조작할 수 없음
        if ("BLINDED".equals(comment.getStatus()) || "DELETED".equals(comment.getStatus())) {
            throw new IllegalStateException("관리자에 의해 조치된 댓글은 수정 또는 삭제할 수 없습니다.");
        }

        comment.updateContent(requestDto.getContent());
        return convertToDtoWithBadges(comment);
    }

    /**
     * [시니어] 댓글 좋아요/취소 토글 로직
     */
    @Transactional
    public void toggleCommentLike(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        // 1. 자기 댓글 좋아요 방지 (운영 정책)
        if (comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("본인의 댓글에는 좋아요를 누를 수 없습니다.");
        }

        java.util.Optional<com.habidue.app.domain.board.CommentLike> existingLike = 
                commentLikeRepository.findByCommentAndUser(comment, currentUser);

        // [시니어 조치] 통계 업데이트를 위한 준비
        User author = comment.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));

        if (existingLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existingLike.get());
            comment.decrementLikeCount();
            stats.decrementCommentLikeReceivedCount();

            // [운영 정책 반영] 댓글 좋아요 취소 시 부여됐던 카르마 회수 (-0.1P = 1포인트 차감)
            karmaService.manualAdjustKarmaRaw(comment.getAuthor().getId(), -1, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED, "댓글 좋아요 취소 (ID: " + comment.getId() + ")", null, false);
        } else {
            // 좋아요 추가
            com.habidue.app.domain.board.CommentLike newLike = com.habidue.app.domain.board.CommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();
            commentLikeRepository.save(newLike);
            comment.incrementLikeCount();
            stats.incrementCommentLikeReceivedCount();

            // [운영 정책 반영] 댓글 좋아요 획득 시 작성자 카르마 +1포인트(0.1점) 부여
            // 댓글 하나당 최대 1.0점(10포인트)까지만 획득 가능하도록 설정
            String postKey = "COMMENT_ID: " + comment.getId();
            karmaService.restoreKarma(comment.getAuthor().getId(), 1, postKey, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
        }
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats);
    }
}
