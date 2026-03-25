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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.habidue.app.service.user.ExpService;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.service.user.KarmaService;

@Slf4j
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
    private final ExpService expService;
    private final KarmaService karmaService;
    private final com.habidue.app.service.ranking.RankingService rankingService;
    private final com.habidue.app.repository.board.CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    private static final String LIKE_NOTI_COOLDOWN_KEY = "like:noti:cooldown:";

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto) {
        User author = getCurrentUser();
        if (karmaService.isRestricted(author)) throw new IllegalArgumentException("활동 제한 상태입니다.");
        Post post = postRepository.findById(postId).orElseThrow();
        Comment parent = requestDto.getParentId() != null ? commentRepository.findById(requestDto.getParentId()).orElseThrow() : null;
        Comment comment = Comment.builder().content(requestDto.getContent()).post(post).author(author).parent(parent).build();
        postRepository.incrementCommentCount(postId);
        
        // [복구] 유저 활동 통계 업데이트 및 배지 체크
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        stats.incrementCommentCount();
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats);

        Comment savedComment = commentRepository.save(comment);
        
        // [시니어 조치] 실시간 급상승 랭킹 점수 반영 (댓글 작성 +10점)
        if (post.getNotice() != null) {
            rankingService.increaseNoticeScore(post.getNotice().getId(), com.habidue.app.service.ranking.RankingService.SCORE_COMMENT);
        }
        
        // 경험치 증여
        expService.grantExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 작성: " + savedComment.getId());
        
        // 알림 발송 (예외가 발생해도 댓글 작성은 성공해야 함)
        try {
            sendCommentNotification(savedComment, author);
        } catch (Exception e) {
            log.error("Error sending comment notification: {}", e.getMessage());
        }

        // [시니어 조치] 새 댓글은 자식이 없으므로 복잡한 재귀 없이 단순 변환하여 반환
        return convertToSimpleDto(savedComment);
    }

    /**
     * [시니어 조치] 새 댓글 작성을 위한 가벼운 DTO 변환 (순환 참조 및 재귀 방지)
     */
    private CommentResponseDto convertToSimpleDto(Comment comment) {
        boolean isAdmin = false;
        Long currentUserId = null;
        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                currentUserId = principal.getId();
                isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
        } catch (Exception e) {}

        // from 메서드 대신 직접 빌더 사용 (재귀 차단)
        CommentResponseDto dto = CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getNickname() != null ? comment.getAuthor().getNickname() : comment.getAuthor().getUsername())
                .authorLevel(comment.getAuthor().getLevel())
                .authorExp(comment.getAuthor().getTotalExp())
                .authorKarmaPoint(comment.getAuthor().getKarmaPoint())
                .targetAuthorName(comment.getParent() != null ? 
                        (comment.getParent().getAuthor().getNickname() != null ? comment.getParent().getAuthor().getNickname() : comment.getParent().getAuthor().getUsername()) 
                        : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .postId(comment.getPost().getId())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt().toString())
                .likeCount(0)
                .children(new java.util.ArrayList<>())
                .build();
        
        dto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(comment.getAuthor()).stream().map(ub -> {
            String badgeType = ub.getBadge().getCode().replace("_BASE", "");
            com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
            return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, rule != null ? rule.getFullDisplayName() : ub.getBadge().getName());
        }).collect(java.util.stream.Collectors.toList()));
        
        return dto;
    }

    private void sendCommentNotification(Comment comment, User author) {
        Post post = comment.getPost();
        if (comment.getParent() != null) {
            User parentAuthor = comment.getParent().getAuthor();
            if (!parentAuthor.getId().equals(author.getId())) {
                notificationService.send(parentAuthor, NotificationType.REPLY, String.format("↪️ 회원님의 댓글에 새로운 답글이 달렸습니다: \"%s\"", truncateContent(comment.getContent())), comment.getId(), post.getId());
            }
        } else {
            User postAuthor = post.getAuthor();
            if (!postAuthor.getId().equals(author.getId())) {
                notificationService.send(postAuthor, NotificationType.COMMENT, String.format("💬 회원님의 게시글에 새로운 댓글이 달렸습니다: \"%s\"", truncateContent(comment.getContent())), comment.getId(), post.getId());
            }
        }
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 20 ? content.substring(0, 20) + "..." : content;
    }

    private CommentResponseDto convertToDtoWithBadges(Comment comment) {
        boolean isAdmin = false;
        Long currentUserId = null;
        try {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                currentUserId = principal.getId();
                isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
        } catch (Exception e) {
            log.error("Error getting current user for comment DTO: {}", e.getMessage());
        }
        
        CommentResponseDto dto = CommentResponseDto.from(comment, isAdmin, currentUserId, commentLikeRepository);
        dto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(comment.getAuthor()).stream().map(ub -> {
            String badgeType = ub.getBadge().getCode().replace("_BASE", "");
            com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
            return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, rule != null ? rule.getFullDisplayName() : ub.getBadge().getName());
        }).collect(java.util.stream.Collectors.toList()));
        return dto;
    }

    @Transactional
    public void toggleCommentLike(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (comment.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("본인 댓글 좋아요 불가");

        java.util.Optional<com.habidue.app.domain.board.CommentLike> existingLike = commentLikeRepository.findByCommentAndUser(comment, currentUser);
        User author = comment.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        String postKey = "COMMENT_ID: " + comment.getId();

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.decrementLikeCount();
            stats.decrementCommentLikeReceivedCount(); 
            
            // [시니어 조치] 댓글 좋아요 취소 시 경험치 회수
            expService.revokeExp(author.getId(), ExpReason.RECEIVED_LIKE, "댓글 좋아요 취소: " + comment.getId());
            
            karmaService.revokeKarma(author.getId(), 1, postKey, com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED);
        } else {
            commentLikeRepository.save(com.habidue.app.domain.board.CommentLike.builder().comment(comment).user(currentUser).build());
            comment.incrementLikeCount();
            stats.incrementCommentLikeReceivedCount(); // [복구]
            
            // 신뢰 점수 회복 시도
            int gain = 0;
            try {
                gain = karmaService.restoreKarma(author.getId(), 1, postKey, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
                
                // [시니어 조치] 댓글 좋아요 수신 시 경험치(열정파) 부여
                expService.grantExp(author.getId(), ExpReason.RECEIVED_LIKE, "댓글 좋아요 수신: " + comment.getId());
            } catch (Exception e) {
                log.error("Error restoring karma for like: {}", e.getMessage());
            }

            // 알림 발송 (Redis 쿨타임 적용)
            try {
                String cooldownKey = LIKE_NOTI_COOLDOWN_KEY + currentUser.getId() + ":COMMENT:" + comment.getId();
                Boolean canSendNoti = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "true", 1, TimeUnit.HOURS);

                if (Boolean.TRUE.equals(canSendNoti)) {
                    String truncatedComment = truncateContent(comment.getContent());
                    String notiContent = String.format("❤️ '%s'님이 회원님의 댓글을 좋아합니다.", currentUser.getNickname());
                    if (gain > 0) notiContent += " (⚖️ 신뢰 점수 +0.1P)";
                    
                    // relatedTargetId에 comment.getId()를 전달하여 프론트에서 포커싱 가능하게 함
                    notificationService.send(author, NotificationType.SYSTEM, notiContent + ": \"" + truncatedComment + "\"", comment.getId(), comment.getPost().getId());
                }
            } catch (Exception e) {
                log.error("Error sending like notification: {}", e.getMessage());
            }
        }
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats); // [복구]
    }

    public Page<CommentResponseDto> getComments(Long postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);
        return comments.map(this::convertToDtoWithBadges);
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("수정 권한 없음");
        comment.updateContent(requestDto.getContent());
        return convertToDtoWithBadges(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("권한 없음");
        
        User author = comment.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId()).orElse(null);

        // [시니어 조치] 경험치 회수 (댓글 작성 및 해당 댓글이 받았던 좋아요 경험치 모두 회수)
        expService.revokeExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 삭제: " + comment.getId());
        for (int i = 0; i < comment.getLikeCount(); i++) {
            expService.revokeExp(author.getId(), ExpReason.RECEIVED_LIKE, "댓글 삭제에 따른 좋아요 경험치 회수");
        }

        // [시니어 조치] 카르마 회수
        if (comment.getLikeCount() > 0) {
            karmaService.revokeKarma(author.getId(), comment.getLikeCount(), "COMMENT_ID: " + comment.getId(), com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED);
        }

        // [시니어 조치] 실시간 급상승 랭킹 점수 차감 (공고글일 경우)
        if (comment.getPost().getNotice() != null) {
            rankingService.increaseNoticeScore(comment.getPost().getNotice().getId(), -com.habidue.app.service.ranking.RankingService.SCORE_COMMENT);
        }

        // [시니어 조치] 유저 활동 통계 차감 (마이페이지 반영)
        if (stats != null) {
            stats.decrementCommentCount();
            // 댓글이 받았던 좋아요 수만큼 통계 차감
            for (int i = 0; i < comment.getLikeCount(); i++) {
                stats.decrementCommentLikeReceivedCount();
            }
            userActivityStatsRepository.save(stats);
        }

        postRepository.decrementCommentCount(comment.getPost().getId());
        commentRepository.delete(comment);
    }
}
