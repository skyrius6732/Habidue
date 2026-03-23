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
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
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
    private final NotificationService notificationService; // [시니어 조치] 알림 서비스 추가

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

        postRepository.incrementCommentCount(postId);

        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        
        stats.incrementCommentCount();
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats);

        Comment savedComment = commentRepository.save(comment);

        expService.grantExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 작성: " + savedComment.getId());

        // [시니어 조치] 알림 발송
        sendCommentNotification(savedComment, author);

        return convertToDtoWithBadges(savedComment);
    }

    /**
     * 댓글/답글 알림 발송 로직
     */
    private void sendCommentNotification(Comment comment, User author) {
        Post post = comment.getPost();
        
        if (comment.getParent() != null) {
            // 1. 답글 알림: 부모 댓글 작성자에게 전송
            User parentAuthor = comment.getParent().getAuthor();
            if (!parentAuthor.getId().equals(author.getId())) {
                String content = String.format("↪️ 회원님의 댓글에 새로운 답글이 달렸습니다: \"%s\"", 
                        truncateContent(comment.getContent()));
                notificationService.send(parentAuthor, NotificationType.REPLY, content, post.getId());
            }
        } else {
            // 2. 일반 댓글 알림: 게시글 작성자에게 전송
            User postAuthor = post.getAuthor();
            if (!postAuthor.getId().equals(author.getId())) {
                String content = String.format("💬 회원님의 게시글에 새로운 댓글이 달렸습니다: \"%s\"", 
                        truncateContent(comment.getContent()));
                notificationService.send(postAuthor, NotificationType.COMMENT, content, post.getId());
            }
        }
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }

    public Page<CommentResponseDto> getComments(Long postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);
        return comments.map(this::convertToDtoWithBadges);
    }

    private CommentResponseDto convertToDtoWithBadges(Comment comment) {
        boolean isAdmin = false;
        Long currentUserId = null;
        try {
            User currentUser = getCurrentUser();
            isAdmin = currentUser.getRole() == com.habidue.app.domain.user.Role.ADMIN;
            currentUserId = currentUser.getId();
        } catch (Exception e) {}

        CommentResponseDto dto = CommentResponseDto.from(comment, isAdmin, currentUserId, commentLikeRepository);

        dto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(comment.getAuthor()).stream()
                .map(ub -> {
                    String badgeType = ub.getBadge().getCode().replace("_BASE", "");
                    com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
                    String realName = (rule != null) ? rule.getFullDisplayName() : ub.getBadge().getName();
                    return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, realName);
                })
                .collect(java.util.stream.Collectors.toList()));

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

        if ("BLINDED".equalsIgnoreCase(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.");
        }

        if ("BLINDED".equals(comment.getStatus()) || "DELETED".equals(comment.getStatus())) {
            throw new IllegalStateException("관리자에 의해 조치된 댓글은 수정 또는 삭제할 수 없습니다.");
        }

        postRepository.decrementCommentCount(comment.getPost().getId());
        commentRepository.delete(comment);

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

        if ("BLINDED".equalsIgnoreCase(comment.getPost().getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 차단된 게시글의 댓글은 등록, 수정, 삭제할 수 없습니다.");
        }

        if ("BLINDED".equals(comment.getStatus()) || "DELETED".equals(comment.getStatus())) {
            throw new IllegalStateException("관리자에 의해 조치된 댓글은 수정 또는 삭제할 수 없습니다.");
        }

        comment.updateContent(requestDto.getContent());
        return convertToDtoWithBadges(comment);
    }

    @Transactional
    public void toggleCommentLike(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("본인의 댓글에는 좋아요를 누를 수 없습니다.");
        }

        java.util.Optional<com.habidue.app.domain.board.CommentLike> existingLike = 
                commentLikeRepository.findByCommentAndUser(comment, currentUser);

        User author = comment.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.decrementLikeCount();
            stats.decrementCommentLikeReceivedCount();
            karmaService.manualAdjustKarmaRaw(comment.getAuthor().getId(), -1, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED, "댓글 좋아요 취소 (ID: " + comment.getId() + ")", null, false);
        } else {
            com.habidue.app.domain.board.CommentLike newLike = com.habidue.app.domain.board.CommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();
            commentLikeRepository.save(newLike);
            comment.incrementLikeCount();
            stats.incrementCommentLikeReceivedCount();

            String postKey = "COMMENT_ID: " + comment.getId();
            karmaService.restoreKarma(comment.getAuthor().getId(), 1, postKey, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
        }
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats);
    }
}
