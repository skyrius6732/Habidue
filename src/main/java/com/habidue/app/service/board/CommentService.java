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
    private final jakarta.persistence.EntityManager entityManager;

    private static final String LIKE_NOTI_COOLDOWN_KEY = "like:noti:cooldown:";

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getMyComments(Long authorId, Pageable pageable) {
        return commentRepository.findByAuthorIdAndStatus(authorId, "ACTIVE", pageable)
                .map(comment -> CommentResponseDto.from(comment, false, authorId, commentLikeRepository));
    }

    @Transactional
    public CommentResponseDto createComment(Long postId, CommentRequestDto requestDto) {
        // [시니어 조치] 최후의 수단: 현재 세션의 오염된 프록시(특히 User #1)를 모두 비우고 시작
        entityManager.clear();

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        if (karmaService.isRestricted(author)) throw new IllegalArgumentException("활동 제한 상태입니다.");
        
        // [시니어 조치] 선제적 Fetch Join으로 캐시 채우기
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        
        Comment parent = null;
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findWithAuthorById(requestDto.getParentId())
                    .orElseThrow(() -> new NoSuchElementException("부모 댓글을 찾을 수 없습니다."));
        }

        Comment comment = Comment.builder().content(requestDto.getContent()).post(post).author(author).parent(parent).build();
        postRepository.incrementCommentCount(postId);

        userActivityStatsRepository.insertIgnore(author.getId());
        userActivityStatsRepository.incrementCommentCount(author.getId());
        UserActivityStats freshStats = userActivityStatsRepository.findById(author.getId()).orElseThrow();
        badgeService.checkAndAwardBadges(freshStats);
        
        Comment savedComment = commentRepository.save(comment);
        commentRepository.flush();

        // [시니어 조치] 모든 정보를 포함하여 다시 조회
        Comment freshComment = commentRepository.findByIdWithAllInfo(savedComment.getId())
                .orElseThrow(() -> new NoSuchElementException("저장된 댓글을 찾을 수 없습니다."));
        
        if (post.getNotice() != null) {
            rankingService.increaseNoticeScore(post.getNotice().getId(), com.habidue.app.service.ranking.RankingService.SCORE_COMMENT);
        }
        
        expService.grantExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 작성: " + freshComment.getId());
        
        // [시니어 조치] 알림 발송 전 unproxy 처리
        try {
            sendCommentNotification(freshComment);
        } catch (Exception e) {
            log.error("Error sending comment notification: {}", e.getMessage());
        }

        return convertToSimpleDto(freshComment);
    }

    /**
     * [시니어 조치] 프록시 문제를 완벽히 해결하기 위한 안전한 DTO 변환
     */
    private CommentResponseDto convertToSimpleDto(Comment comment) {
        if (comment == null) return null;

        // [핵심] 프록시를 실제 엔티티로 변환하여 세션 의존성 제거
        User author = (User) org.hibernate.Hibernate.unproxy(comment.getAuthor());
        Post post = (Post) org.hibernate.Hibernate.unproxy(comment.getPost());
        User postAuthor = (User) org.hibernate.Hibernate.unproxy(post.getAuthor());
        
        Comment parent = null;
        User parentAuthor = null;
        if (comment.getParent() != null) {
            parent = (Comment) org.hibernate.Hibernate.unproxy(comment.getParent());
            parentAuthor = (User) org.hibernate.Hibernate.unproxy(parent.getAuthor());
        }

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

        // DTO 빌드 (이제 모든 객체는 실제 엔티티이므로 세션 없이도 접근 가능)
        CommentResponseDto dto = CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(author.getId())
                .authorName(author.getNickname() != null ? author.getNickname() : author.getUsername())
                .authorLevel(author.getLevel())
                .authorExp(author.getTotalExp())
                .authorKarmaPoint(author.getKarmaPoint())
                .authorEquippedEffect(author.getEquippedEffect())
                .showLevelEffects(author.isShowLevelEffects())
                .targetAuthorName(parentAuthor != null ? 
                        (parentAuthor.getNickname() != null ? parentAuthor.getNickname() : parentAuthor.getUsername()) 
                        : null)
                .parentId(parent != null ? parent.getId() : null)
                .postId(post.getId())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt().toString())
                .likeCount(0)
                .children(new java.util.ArrayList<>())
                .build();
        
        dto.setAuthorBadges(userBadgeRepository.findByUserOrderByAcquiredAtDesc(author).stream().map(ub -> {
            String badgeType = ub.getBadge().getCode().replace("_BASE", "");
            com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
            return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, rule != null ? rule.getFullDisplayName() : ub.getBadge().getName());
        }).collect(java.util.stream.Collectors.toList()));
        
        return dto;
    }

    private void sendCommentNotification(Comment comment) {
        Post post = (Post) org.hibernate.Hibernate.unproxy(comment.getPost());
        User author = (User) org.hibernate.Hibernate.unproxy(comment.getAuthor());
        
        if (comment.getParent() != null) {
            Comment parent = (Comment) org.hibernate.Hibernate.unproxy(comment.getParent());
            User parentAuthor = (User) org.hibernate.Hibernate.unproxy(parent.getAuthor());
            if (!parentAuthor.getId().equals(author.getId())) {
                notificationService.send(parentAuthor, NotificationType.REPLY, String.format("↪️ 회원님의 댓글에 새로운 답글이 달렸습니다: \"%s\"", truncateContent(comment.getContent())), comment.getId(), post.getId());
            }
        } else {
            User postAuthor = (User) org.hibernate.Hibernate.unproxy(post.getAuthor());
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
        // [시니어 조치] LazyInitializationException 예방
        org.hibernate.Hibernate.initialize(comment.getAuthor());
        if (comment.getParent() != null) {
            org.hibernate.Hibernate.initialize(comment.getParent().getAuthor());
        }

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
        String postKey = "POST_ID: " + comment.getPost().getId();

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            commentRepository.decrementLikeCount(commentId);
            userActivityStatsRepository.decrementCommentLikeReceivedCount(author.getId());

            // [시니어 조치] 댓글 좋아요 취소 시 경험치 회수
            expService.revokeExp(author.getId(), ExpReason.RECEIVED_LIKE, "댓글 좋아요 취소: " + comment.getId());

            // [시니어 조치] KarmaService가 DB에서 직접 잔여 좋아요를 계산 (null 전달)
            karmaService.revokeKarma(author.getId(), 1, postKey, com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED, null, 10);
        } else {
            commentLikeRepository.save(com.habidue.app.domain.board.CommentLike.builder().comment(comment).user(currentUser).build());
            commentRepository.incrementLikeCount(commentId);
            userActivityStatsRepository.incrementCommentLikeReceivedCount(author.getId());

            // 신뢰 점수 회복 시도 (게시글 당 1.0P 상한 공유)
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
        UserActivityStats freshStats = userActivityStatsRepository.findById(author.getId()).orElseThrow();
        badgeService.checkAndAwardBadges(freshStats);
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
        
        // [시니어 조치] 중복 회수 방지 가드: ACTIVE 상태가 아니면 (USER_DELETED, DELETED, BLINDED 등) 모두 스킵
        if (!"ACTIVE".equals(comment.getStatus())) {
            log.info("ACTIVE 상태가 아닌 댓글이므로 회수 로직을 건너뜁니다. ID: {}, status: {}", commentId, comment.getStatus());
            return;
        }
        
        User author = comment.getAuthor();

        // [시니어 조치] 경험치 회수 (댓글 작성 및 해당 댓글이 받았던 좋아요 경험치 모두 회수)
        expService.revokeExp(author.getId(), ExpReason.COMMENT_CREATED, "댓글 삭제: " + comment.getId());
        for (int i = 0; i < comment.getLikeCount(); i++) {
            expService.revokeExp(author.getId(), ExpReason.RECEIVED_LIKE, "댓글 삭제에 따른 좋아요 경험치 회수");
        }

        // [시니어 조치] 카르마 회수 - 상태를 먼저 USER_DELETED로 변경 + flush하여
        // KarmaService.computeRemainingHeartsForPostKey()의 DB 쿼리에서 이 댓글이 제외되도록 함
        int originalLikeCount = comment.getLikeCount();
        comment.changeStatus("USER_DELETED");
        comment.getCommentLikes().clear(); // 연관된 좋아요 물리 삭제
        commentRepository.saveAndFlush(comment);

        if (originalLikeCount > 0) {
            String postKey = "POST_ID: " + comment.getPost().getId();
            karmaService.revokeKarma(author.getId(), originalLikeCount, postKey, com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED, null, 10);
        }

        // [시니어 조치] 실시간 급상승 랭킹 점수 차감 (공고글일 경우)
        if (comment.getPost().getNotice() != null) {
            rankingService.increaseNoticeScore(comment.getPost().getNotice().getId(), -com.habidue.app.service.ranking.RankingService.SCORE_COMMENT);
        }

        // [시니어 조치] 유저 활동 통계 원자적 차감 (마이페이지 반영)
        userActivityStatsRepository.decrementCommentCount(author.getId());
        if (originalLikeCount > 0) userActivityStatsRepository.decrementCommentLikeReceivedCountBy(author.getId(), originalLikeCount);

        postRepository.decrementCommentCount(comment.getPost().getId());
    }
}
