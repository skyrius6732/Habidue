package com.habidue.app.service.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostLike;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.domain.notice.Notice;
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
        if (savedPost.getNotice() != null) {
            savedPost.getNotice().setLastPostAt(LocalDateTime.now());
            // [시니어 조치] 실시간 급상승 랭킹 점수 반영 (소통글 작성 +30점)
            rankingService.increaseNoticeScore(savedPost.getNotice().getId(), com.habidue.app.service.ranking.RankingService.SCORE_POST);
        }
        expService.grantExp(author.getId(), ExpReason.POST_CREATED, "게시글 작성: " + savedPost.getId());
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
        PostResponseDto dto = PostResponseDto.from(post);
        fillAuthorBadgeInfo(dto, post.getAuthor());
        if (currentUser != null) dto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
        return dto;
    }

    @Transactional
    public boolean toggleLike(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        Optional<PostLike> postLike = postLikeRepository.findByPostAndUser(post, user);
        
        boolean isNowLiked;
        if (postLike.isPresent()) {
            postLikeRepository.delete(postLike.get());
            postRepository.decrementLikeCount(postId);
            isNowLiked = false;
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            postRepository.incrementLikeCount(postId);
            isNowLiked = true;
        }

        if (!post.getAuthor().getId().equals(user.getId())) {
            User author = post.getAuthor();
            if (isNowLiked) {
                int gain = karmaService.restoreKarma(author.getId(), 1, "POST_ID: " + postId, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
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
        postRepository.delete(post);
    }
}
