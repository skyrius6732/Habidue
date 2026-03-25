package com.habidue.app.service.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostLike;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.ExpCategory;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.dto.board.PostRequestDto;
import com.habidue.app.dto.board.PostResponseDto;
import com.habidue.app.repository.board.PostLikeRepository;
import com.habidue.app.repository.board.PostRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.badge.UserBadgeRepository;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.repository.tag.TagRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.service.badge.BadgeService;
import com.habidue.app.service.tag.TagService;
import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.habidue.app.service.user.ExpService;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.service.user.KarmaService;
import com.habidue.app.service.ranking.RankingService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final NoticeRepository noticeRepository;
    private final TagRepository tagRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final com.habidue.app.repository.badge.BadgeLevelRuleRepository badgeLevelRuleRepository;
    private final BadgeService badgeService;
    private final UserBadgeRepository userBadgeRepository;
    private final ExpService expService;
    private final KarmaService karmaService;
    private final NotificationService notificationService;
    private final com.habidue.app.service.ranking.RankingService rankingService;
    private final StringRedisTemplate redisTemplate;

    private static final String LIKE_NOTI_COOLDOWN_KEY = "like:noti:cooldown:";

    /**
     * [시니어 조치] 사용자 규칙에 따른 정밀 경험치 사유 매핑 (Exact Match)
     */
    private ExpReason determineExpReason(PostType type, String category) {
        if (category == null) return ExpReason.POST_CREATED;

        // 1. 공고 소통방 (NOTICE) -> 전체 지식인 (+30)
        if (type == PostType.NOTICE) return ExpReason.KNOWLEDGE_SHARE;

        // 2. 통합광장 (GENERAL) 분기
        if (type == PostType.GENERAL) {
            // 지식인 (+30): 서류현황, 기관별팁, 기관문의
            if (java.util.List.of("STATUS", "INST_TIPS", "INQUIRY").contains(category)) {
                return ExpReason.KNOWLEDGE_SHARE;
            }
            // 그 외 (동네소식, 장소추천, 동네번개, 자유수다) -> 열정파 (+10)
            return ExpReason.POST_CREATED;
        }

        // 3. 당첨후기 (REVIEW) 분기
        if (type == PostType.REVIEW) {
            // 지식인 (+30): 당첨비결, 서류팁, 자금계획, 일반팁
            if (java.util.List.of("SECRET", "DOCUMENT", "LOAN", "TIPS").contains(category)) {
                return ExpReason.KNOWLEDGE_SHARE;
            }
            // 리뷰왕 (+40): 내집마련기, 사전점검, 입주후기
            if (java.util.List.of("SUCCESS_STORY", "MOVE_IN", "PARTNER_REVIEW").contains(category)) {
                return ExpReason.PRODUCT_REVIEW;
            }
        }

        // 4. 하비 파트너스 (PARTNER) -> 전체 리뷰왕 (+40)
        // (단, 파트너스 내 자유수다(FREE)는 열정파로 처리)
        if (type == PostType.PARTNER) {
            if ("FREE".equals(category)) return ExpReason.POST_CREATED;
            return ExpReason.PRODUCT_REVIEW;
        }

        // 기본값: 열정파 (+10)
        return ExpReason.POST_CREATED;
    }

    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, UserPrincipal currentUser) {
        User author = userRepository.findById(currentUser.getId()).orElseThrow();
        if (karmaService.isRestricted(author)) throw new IllegalArgumentException("활동 제한 상태입니다.");
        Notice notice = null;
        if (requestDto.getType() == PostType.NOTICE) notice = noticeRepository.findById(requestDto.getNoticeId()).orElseThrow();
        Post post = Post.builder().title(requestDto.getTitle()).content(requestDto.getContent()).type(requestDto.getType()).category(requestDto.getCategory()).subCategory(requestDto.getSubCategory()).regionTag(requestDto.getRegionTag()).notice(notice).author(author).build();
        if (requestDto.getImageUrls() != null) {
            for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder().post(post).imageUrl(requestDto.getImageUrls().get(i)).sortOrder(i).build());
            }
        }
        if (requestDto.getTagIds() != null) {
            tagRepository.findAllById(requestDto.getTagIds()).forEach(tag -> post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag)));
        }
        Post savedPost = postRepository.save(post);
        
        // [시니어 조치] 활동 통계 업데이트 (마이페이지 게시글 수 반영)
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        stats.incrementPostCount();
        if (requestDto.getType() == PostType.REVIEW) stats.incrementReviewPostCount();
        userActivityStatsRepository.save(stats);

        if (savedPost.getNotice() != null) {
            savedPost.getNotice().setLastPostAt(LocalDateTime.now());
            // [시니어 조치] 실시간 급상승 랭킹 점수 반영 (소통글 작성 +30점)
            rankingService.increaseNoticeScore(savedPost.getNotice().getId(), com.habidue.app.service.ranking.RankingService.SCORE_POST);
        }

        // [시니어 조치] 사용자 규칙에 따른 정밀 분야별 경험치 부여
        ExpReason reason = determineExpReason(requestDto.getType(), requestDto.getCategory());
        expService.grantExp(author.getId(), reason, "게시글 작성: " + savedPost.getId());

        // [시니어 조치] 사진 첨부 시 해당 분야의 사진 보너스 부여
        if (requestDto.getImageUrls() != null && !requestDto.getImageUrls().isEmpty()) {
            ExpReason photoReason = ExpReason.SINCERITY_PHOTO_BONUS;
            if (reason.getCategory() == ExpCategory.KNOWLEDGE) photoReason = ExpReason.KNOWLEDGE_PHOTO_BONUS;
            else if (reason.getCategory() == ExpCategory.REVIEW) photoReason = ExpReason.REVIEW_PHOTO_BONUS;
            
            expService.grantExp(author.getId(), photoReason, "사진 첨부 보너스: " + savedPost.getId());
        }

        return PostResponseDto.from(savedPost);
    }

    @Transactional
    public PostResponseDto updatePost(Long postId, PostRequestDto requestDto, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("수정 권한 없음");
        post.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getCategory(), requestDto.getSubCategory(), requestDto.getRegionTag());
        post.getImages().clear();
        if (requestDto.getImageUrls() != null) {
            for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder().post(post).imageUrl(requestDto.getImageUrls().get(i)).sortOrder(i).build());
            }
        }
        post.clearTags();
        if (requestDto.getTagIds() != null) {
            tagRepository.findAllById(requestDto.getTagIds()).forEach(tag -> post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag)));
        }
        return PostResponseDto.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(PostType type, Long noticeId, String category, String subCategory, String keyword, String regionTag, String tagName, UserPrincipal currentUser, Pageable pageable) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Page<Post> posts = postRepository.findPosts(type, noticeId, category, subCategory, keyword, "ACTIVE", regionTag, tagName, pageable, currentUserId, isAdmin);
        return posts.map(post -> {
            post.getImages().size();
            post.getTags().forEach(pt -> pt.getTag().getName());
            PostResponseDto dto = PostResponseDto.from(post);
            fillAuthorBadgeInfo(dto, post.getAuthor());
            if (currentUser != null) dto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopPosts(Long noticeId, int limit) {
        return postRepository.findTopByNoticeIdOrderByPopularity(noticeId, org.springframework.data.domain.PageRequest.of(0, limit)).stream().map(post -> {
            PostResponseDto dto = PostResponseDto.from(post);
            fillAuthorBadgeInfo(dto, post.getAuthor());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTrendingPosts(int limit) {
        return postRepository.findTrendingPosts(limit).stream().map(post -> {
            PostResponseDto dto = PostResponseDto.from(post);
            fillAuthorBadgeInfo(dto, post.getAuthor());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countByKeywordAndType(String keyword, PostType type, UserPrincipal currentUser) {
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return postRepository.countByKeywordAndType(keyword, type, currentUser != null ? currentUser.getId() : null, isAdmin);
    }

    @Transactional
    public PostResponseDto getPostDetail(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findWithAuthorById(postId).orElseThrow();
        post.getImages().size();
        post.getTags().forEach(pt -> pt.getTag().getName());
        postRepository.incrementViewCount(postId);

        // [복구] 작성자 활동 통계 업데이트 (조회수 수신)
        userActivityStatsRepository.findById(post.getAuthor().getId()).ifPresent(stats -> {
            stats.incrementViewReceivedCount();
            userActivityStatsRepository.save(stats);
        });

        PostResponseDto dto = PostResponseDto.from(post);
        fillAuthorBadgeInfo(dto, post.getAuthor());
        
        // [시니어 조치] ID 0 또는 null 방지 로직 적용
        if (currentUser != null && currentUser.getId() != null && currentUser.getId() > 0) {
            dto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
        }
        return dto;
    }

    @Transactional
    public boolean toggleLike(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        Optional<PostLike> postLike = postLikeRepository.findByPostAndUser(post, user);
        
        boolean isNowLiked;
        User author = post.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));

        if (postLike.isPresent()) {
            postLikeRepository.delete(postLike.get());
            postRepository.decrementLikeCount(postId);
            stats.decrementPostLikeReceivedCount(); 
            
            // [시니어 조치] 좋아요 취소 시 경험치 회수 추가
            ExpReason baseReason = determineExpReason(post.getType(), post.getCategory());
            ExpReason likeReason = (baseReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE :
                                   (baseReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE : ExpReason.RECEIVED_LIKE;
            expService.revokeExp(author.getId(), likeReason, "좋아요 취소: " + post.getId());
            
            isNowLiked = false;
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            postRepository.incrementLikeCount(postId);
            stats.incrementPostLikeReceivedCount(); // [복구]
            isNowLiked = true;
        }
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats); // [복구]

        if (!post.getAuthor().getId().equals(user.getId())) {
            if (isNowLiked) {
                int gain = karmaService.restoreKarma(author.getId(), 1, "POST_ID: " + postId, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
                
                // [시니어 조치] 게시글 성격에 따른 분야별 경험치(EXP) 부여
                ExpReason likeReason = ExpReason.RECEIVED_LIKE; // 기본: 열정파
                ExpReason baseReason = determineExpReason(post.getType(), post.getCategory());
                if (baseReason.getCategory() == ExpCategory.KNOWLEDGE) likeReason = ExpReason.KNOWLEDGE_LIKE;
                else if (baseReason.getCategory() == ExpCategory.REVIEW) likeReason = ExpReason.REVIEW_LIKE;
                
                expService.grantExp(author.getId(), likeReason, "좋아요 수신: " + post.getId());

                String cooldownKey = LIKE_NOTI_COOLDOWN_KEY + user.getId() + ":POST:" + post.getId();
                Boolean canSendNoti = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "true", 1, TimeUnit.HOURS);
                if (Boolean.TRUE.equals(canSendNoti)) {
                    String notiContent = String.format("❤️ '%s'님이 회원님의 게시글을 좋아합니다.", user.getNickname());
                    if (gain > 0) notiContent += " (⚖️ 신뢰 점수 +0.1P)";
                    notificationService.send(author, NotificationType.SYSTEM, notiContent + ": \"" + post.getTitle() + "\"", post.getId(), post.getId());
                }
            } else {
                karmaService.revokeKarma(author.getId(), 1, "POST_ID: " + postId, com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED);
            }
        }
        return isNowLiked;
    }

    @Transactional(readOnly = true)
    public com.habidue.app.dto.search.SearchResponseDto searchAll(String keyword, Pageable pageable, UserPrincipal currentUser) {
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Page<Post> postsPage = postRepository.findPosts(null, null, null, null, keyword, "ACTIVE", null, null, pageable, currentUser != null ? currentUser.getId() : null, isAdmin);
        return com.habidue.app.dto.search.SearchResponseDto.builder().keyword(keyword).totalCount((int) postsPage.getTotalElements()).build();
    }

    @Transactional(readOnly = true)
    public List<com.habidue.app.dto.tag.TagResponseDto> getPopularKeywords(int limit) {
        return tagService.getPopularTags(limit);
    }

    private void fillAuthorBadgeInfo(PostResponseDto dto, User author) {
        List<com.habidue.app.dto.badge.BadgeResponseDto> badgeDtos = userBadgeRepository.findByUserOrderByAcquiredAtDesc(author).stream().map(ub -> {
            String badgeType = ub.getBadge().getCode().replace("_BASE", "");
            com.habidue.app.domain.badge.BadgeLevelRule rule = badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
            return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, rule != null ? rule.getFullDisplayName() : ub.getBadge().getName());
        }).collect(Collectors.toList());
        dto.setAuthorBadges(badgeDtos);
    }

    @Transactional
    public void deletePost(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("삭제 권한 없음");
        
        User author = post.getAuthor();
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId()).orElse(null);

        // 1. 활동 통계 차감
        if (stats != null) {
            stats.decrementPostCount();
            if (post.getType() == PostType.REVIEW) stats.decrementReviewPostCount();
            
            // 게시글이 받았던 좋아요 수만큼 통계 차감
            for (int i = 0; i < post.getLikeCount(); i++) {
                stats.decrementPostLikeReceivedCount();
            }
            userActivityStatsRepository.save(stats);
        }

        // 2. 경험치 및 카르마 회수 (게시글)
        ExpReason postReason = determineExpReason(post.getType(), post.getCategory());
        expService.revokeExp(author.getId(), postReason, "게시글 삭제: " + post.getId());
        
        // [시니어 조치] 사진 보너스 경험치 회수 추가 (-10점)
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            ExpReason photoReason = ExpReason.SINCERITY_PHOTO_BONUS;
            if (postReason.getCategory() == ExpCategory.KNOWLEDGE) photoReason = ExpReason.KNOWLEDGE_PHOTO_BONUS;
            else if (postReason.getCategory() == ExpCategory.REVIEW) photoReason = ExpReason.REVIEW_PHOTO_BONUS;
            expService.revokeExp(author.getId(), photoReason, "게시글 삭제에 따른 사진 보너스 회수");
        }

        // 좋아요 수신 경험치 회수
        ExpReason likeReason = (postReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE :
                               (postReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE : ExpReason.RECEIVED_LIKE;
        for (int i = 0; i < post.getLikeCount(); i++) {
            expService.revokeExp(author.getId(), likeReason, "게시글 삭제에 따른 좋아요 경험치 회수");
        }

        if (post.getLikeCount() > 0) {
            karmaService.revokeKarma(author.getId(), post.getLikeCount(), "POST_ID: " + post.getId(), com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED);
        }

        // 3. 댓글 관련 연쇄 회수
        if (post.getComments() != null) {
            post.getComments().forEach(comment -> {
                User cAuthor = comment.getAuthor();
                userActivityStatsRepository.findById(cAuthor.getId()).ifPresent(cStats -> {
                    cStats.decrementCommentCount();
                    for (int i = 0; i < comment.getLikeCount(); i++) {
                        cStats.decrementCommentLikeReceivedCount();
                    }
                    userActivityStatsRepository.save(cStats);
                });
                
                expService.revokeExp(cAuthor.getId(), ExpReason.COMMENT_CREATED, "게시글 삭제에 따른 댓글 자동 회수");
                for (int i = 0; i < comment.getLikeCount(); i++) {
                    expService.revokeExp(cAuthor.getId(), ExpReason.RECEIVED_LIKE, "게시글 삭제에 따른 댓글 좋아요 회수");
                }
                
                if (comment.getLikeCount() > 0) {
                    karmaService.revokeKarma(cAuthor.getId(), comment.getLikeCount(), "COMMENT_ID: " + comment.getId(), com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED);
                }
            });
        }

        // 4. 실시간 급상승 랭킹 차감
        if (post.getNotice() != null) {
            rankingService.increaseNoticeScore(post.getNotice().getId(), -com.habidue.app.service.ranking.RankingService.SCORE_POST);
            if (post.getCommentCount() > 0) {
                rankingService.increaseNoticeScore(post.getNotice().getId(), -(com.habidue.app.service.ranking.RankingService.SCORE_COMMENT * post.getCommentCount()));
            }
        }

        postRepository.delete(post);
    }
}
