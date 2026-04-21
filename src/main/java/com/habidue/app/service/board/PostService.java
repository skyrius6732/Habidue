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
import com.habidue.app.repository.board.CommentRepository;
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
    private final com.habidue.app.repository.barter.TradeProposalRepository tradeProposalRepository;
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
    private final com.habidue.app.service.barter.BarterService barterService;
    private final StringRedisTemplate redisTemplate;
    private final jakarta.persistence.EntityManager entityManager;

    private static final String LIKE_NOTI_COOLDOWN_KEY = "like:noti:cooldown:";

    /**
     * [시니어 조치] 사용자 규칙에 따른 정밀 경험치 사유 매핑 (Exact Match)
     * category 필드에 저장된 소통 주제(Topic) 코드값을 기준으로 판별
     */
    private ExpReason determineExpReason(PostType type, String category) {
        if (category == null) return ExpReason.POST_CREATED;

        // 1. 지식인 (+30) - 공고소통방 전체 또는 특정 정보 주제
        if (type == PostType.NOTICE) return ExpReason.KNOWLEDGE_SHARE;
        if (java.util.List.of("STATUS", "INST_TIPS", "INQUIRY", "SECRET", "DOCUMENT", "LOAN", "TIPS", "GENERAL_TIPS").contains(category)) {
            return ExpReason.KNOWLEDGE_SHARE;
        }

        // 2. 리뷰왕 (+40) - 경험/후기 주제 및 파트너스 이용후기
        if (java.util.List.of("SUCCESS_STORY", "MOVE_IN", "PARTNER_REVIEW", "PRE_CHECK", "POST_CHECK", "ESTIMATE", "CHECKLIST", "MOVING", "CLEANING", "INTERIOR").contains(category)) {
            return ExpReason.PRODUCT_REVIEW;
        }

        // 3. 열정파 (+10) - 그 외 일상 주제 및 기본값
        return ExpReason.POST_CREATED;
    }

    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, UserPrincipal currentUser) {
        User author = userRepository.findById(currentUser.getId()).orElseThrow();
        // [시니어 조치] 오염된 프록시를 방지하기 위해 DB의 최신 상태로 강제 동기화
        entityManager.refresh(author);
        
        if (karmaService.isRestricted(author)) throw new IllegalArgumentException("활동 제한 상태입니다.");
        Notice notice = null;
        if (requestDto.getType() == PostType.NOTICE) notice = noticeRepository.findById(requestDto.getNoticeId()).orElseThrow();
        Post post = Post.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .category(requestDto.getCategory())
                .subCategory(requestDto.getSubCategory())
                .regionTag(requestDto.getRegionTag())
                .notice(notice)
                .author(author)
                .itemName(requestDto.getItemName())
                .wantedItem(requestDto.getWantedItem())
                .itemCondition(requestDto.getItemCondition())
                .barterStatus(com.habidue.app.domain.barter.BarterStatus.TRADING)
                .preferredMethod(requestDto.getPreferredMethod())
                .preferredDate(requestDto.getPreferredDate())
                .preferredTime(requestDto.getPreferredTime())
                .build();
        if (requestDto.getImageUrls() != null) {
            for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder().post(post).imageUrl(requestDto.getImageUrls().get(i)).sortOrder(i).build());
            }
        }
        if (requestDto.getTagIds() != null) {
            tagRepository.findAllById(requestDto.getTagIds()).forEach(tag -> post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag)));
        }
        Post savedPost = postRepository.save(post);

        // [시니어 조치] 활동 통계 원자적 업데이트 (마이페이지 게시글 수 반영)
        // existsById -> save 패턴 대신 insertIgnore 사용 (StaleObjectStateException 방지)
        userActivityStatsRepository.insertIgnore(author.getId());
        userActivityStatsRepository.incrementPostCount(author.getId());
        if (requestDto.getType() == PostType.REVIEW) userActivityStatsRepository.incrementReviewPostCount(author.getId());

        if (savedPost.getNotice() != null) {            savedPost.getNotice().setLastPostAt(LocalDateTime.now());
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

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getMyPosts(Long authorId, Pageable pageable) {
        return postRepository.findByAuthorIdAndStatus(authorId, "ACTIVE", pageable)
                .map(PostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getMyPostsByType(Long authorId, PostType type, Pageable pageable) {
        Page<PostResponseDto> myPosts = postRepository.findByAuthorIdAndStatus(authorId, "ACTIVE", pageable)
                .map(PostResponseDto::from);

        // type 필터 적용 (null이면 모든 타입)
        if (type != null) {
            List<PostResponseDto> filtered = myPosts.stream()
                    .filter(post -> post.getType() == type)
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }
        return myPosts;
    }

    @Transactional
    public PostResponseDto updatePost(Long postId, PostRequestDto requestDto, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        // [시니어 조치] 수정 전 게시글 상태와 작성자 정보를 최신화
        entityManager.refresh(post);
        
        if (!post.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("수정 권한 없음");

        // [시니어 조치] 엔티티 시그니처 변경에 따른 호출부 보정 (타입 유지)
        post.update(requestDto.getTitle(), requestDto.getContent(), post.getType(), requestDto.getCategory(), requestDto.getSubCategory(), requestDto.getRegionTag());
        
        // 물물교환 필드 업데이트
        post.updateBarter(requestDto.getItemName(), requestDto.getWantedItem(), requestDto.getItemCondition(), post.getBarterStatus(),
                requestDto.getPreferredMethod(), requestDto.getPreferredDate(), requestDto.getPreferredTime());

        // [시니어 조치] 이미지 동기화 (중복 방지, 기존 데이터 보존, 삭제 대응)
        List<String> incomingUrls = requestDto.getImageUrls() != null ? requestDto.getImageUrls() : new java.util.ArrayList<>();
        
        // 1. 기존 이미지 중 요청에 없는 것은 제거 (orphanRemoval=true에 의해 DB에서도 자동 삭제됨)
        post.getImages().removeIf(existingImg -> !incomingUrls.contains(existingImg.getImageUrl()));

        // 2. 요청된 URL 순서대로 처리 (유지 또는 추가)
        for (int i = 0; i < incomingUrls.size(); i++) {
            String url = incomingUrls.get(i);
            int sortOrder = i;
            
            java.util.Optional<com.habidue.app.domain.board.PostImage> existing = post.getImages().stream()
                    .filter(img -> img.getImageUrl().equals(url))
                    .findFirst();

            if (existing.isPresent()) {
                // 이미 존재하면 순서만 최신화
                existing.get().setSortOrder(sortOrder);
            } else {
                // 존재하지 않는 새로운 URL이면 추가
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder()
                        .post(post)
                        .imageUrl(url)
                        .sortOrder(sortOrder)
                        .build());
            }
        }

        if (requestDto.getTagIds() != null) {
            post.clearTags();
            tagRepository.findAllById(requestDto.getTagIds()).forEach(tag -> post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag)));
        }
        return PostResponseDto.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(PostType type, Long noticeId, String category, String subCategory, String keyword, String regionTag, String tagName, UserPrincipal currentUser, Pageable pageable) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Page<Post> posts = postRepository.findPosts(type, noticeId, category, subCategory, keyword, "ACTIVE", regionTag, tagName, pageable, currentUserId, null, isAdmin);
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

        // [추가] Soft delete된 게시글 접근 제어
        if (!"ACTIVE".equals(post.getStatus())) {
            // 관리자가 아니면 접근 불가
            boolean isAdmin = currentUser != null &&
                            userRepository.findById(currentUser.getId())
                                .map(user -> user.getRole() != null &&
                                     user.getRole().getKey().equals("ROLE_ADMIN"))
                                .orElse(false);

            if (!isAdmin) {
                throw new IllegalArgumentException("삭제된 게시글입니다.");
            }
        }

        post.getImages().size();
        post.getTags().forEach(pt -> pt.getTag().getName());
        postRepository.incrementViewCount(postId);

        // [복구] 작성자 활동 통계 원자적 업데이트 (조회수 수신)
        userActivityStatsRepository.incrementViewReceivedCount(post.getAuthor().getId());

        PostResponseDto dto = PostResponseDto.from(post);
        fillAuthorBadgeInfo(dto, post.getAuthor());
        
        // [시니어 조치] 이전글/다음글 ID 조회 추가
        Long noticeId = post.getNotice() != null ? post.getNotice().getId() : null;
        dto.setPrevId(postRepository.findPrevId(post.getId(), post.getType().name(), noticeId));
        dto.setNextId(postRepository.findNextId(post.getId(), post.getType().name(), noticeId));
        
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

        if (postLike.isPresent()) {
            postLikeRepository.delete(postLike.get());
            postRepository.decrementLikeCount(postId);
            // [시니어 조치] DB 직접 수정 쿼리 후 엔티티 새로고침으로 상태 동기화 (음수 발생 버그 방지)
            entityManager.flush();
            entityManager.refresh(post);
            
            userActivityStatsRepository.decrementPostLikeReceivedCount(author.getId());

            // [시니어 조치] 본인이 아닐 때만 경험치 회수 (대칭성 확보)
            if (!author.getId().equals(user.getId())) {
                ExpReason baseReason = determineExpReason(post.getType(), post.getCategory());
                ExpReason likeReason = (baseReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE :
                                       (baseReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE : ExpReason.RECEIVED_LIKE;
                expService.revokeExp(author.getId(), likeReason, "좋아요 취소: " + post.getId());
            }

            isNowLiked = false;
        } else {
            postLikeRepository.save(PostLike.builder().post(post).user(user).build());
            postRepository.incrementLikeCount(postId);
            // [시니어 조치] DB 직접 수정 쿼리 후 엔티티 새로고침으로 상태 동기화
            entityManager.flush();
            entityManager.refresh(post);

            userActivityStatsRepository.incrementPostLikeReceivedCount(author.getId());

            // [시니어 조치] 본인이 아닐 때만 경험치 부여 (대칭성 확보)
            if (!author.getId().equals(user.getId())) {
                ExpReason baseReason = determineExpReason(post.getType(), post.getCategory());
                ExpReason likeReason = (baseReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE :
                                       (baseReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE : ExpReason.RECEIVED_LIKE;
                expService.grantExp(author.getId(), likeReason, "좋아요 수신: " + post.getId());
            }

            isNowLiked = true;
        }
        UserActivityStats freshStats = userActivityStatsRepository.findById(author.getId()).orElseThrow();
        badgeService.checkAndAwardBadges(freshStats);

        if (!post.getAuthor().getId().equals(user.getId())) {
            String postKey = "POST_ID: " + postId;
            if (isNowLiked) {
                int gain = karmaService.restoreKarma(author.getId(), 1, postKey, null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
                
                String cooldownKey = LIKE_NOTI_COOLDOWN_KEY + user.getId() + ":POST:" + post.getId();
                Boolean canSendNoti = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "true", 1, TimeUnit.HOURS);
                if (Boolean.TRUE.equals(canSendNoti)) {
                    String notiContent = String.format("❤️ '%s'님이 회원님의 게시글을 좋아합니다.", user.getNickname());
                    if (gain > 0) notiContent += " (⚖️ 신뢰 점수 +0.1P)";
                    notificationService.send(author, NotificationType.LIKE, notiContent + ": \"" + post.getTitle() + "\"", post.getId(), post.getId());
                }
            } else {
                // [시니어 조치] KarmaService가 DB에서 직접 잔여 좋아요를 계산 (null 전달)
                // decrementLikeCount()가 @Modifying JPQL로 DB 직접 갱신 → getPostLikeCount() JPQL이 정확한 값 읽음
                karmaService.revokeKarma(author.getId(), 1, "POST_ID: " + postId, com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED, null, 10);
            }
        }
        return isNowLiked;
    }

    @Transactional(readOnly = true)
    public com.habidue.app.dto.search.SearchResponseDto searchAll(String keyword, Pageable pageable, UserPrincipal currentUser) {
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Page<Post> postsPage = postRepository.findPosts(null, null, null, null, keyword, "ACTIVE", null, null, pageable, currentUser != null ? currentUser.getId() : null, null, isAdmin);
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

    /**
     * [시니어 조치] 게시글 삭제 또는 카테고리 변경 시 모든 활동 보상을 회수함
     */
    @Transactional
    public void revokePostRewards(Post post) {
        User author = post.getAuthor();
        ExpReason postReason = determineExpReason(post.getType(), post.getCategory());
        
        // 1. 게시글 작성 경험치 회수
        expService.revokeExp(author.getId(), postReason, "활동 취소(삭제/변경): " + post.getId());
        
        // 2. 사진 보너스 회수
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            ExpReason photoReason = ExpReason.SINCERITY_PHOTO_BONUS;
            if (postReason.getCategory() == ExpCategory.KNOWLEDGE) photoReason = ExpReason.KNOWLEDGE_PHOTO_BONUS;
            else if (postReason.getCategory() == ExpCategory.REVIEW) photoReason = ExpReason.REVIEW_PHOTO_BONUS;
            expService.revokeExp(author.getId(), photoReason, "활동 취소에 따른 사진 보너스 회수");
        }

        // 3. 좋아요 경험치 및 카르마 회수
        // [시니어 조치] 부여할 때와 동일한 정밀 카테고리별 회수 적용
        ExpReason likeReason = (postReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE :
                               (postReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE : ExpReason.RECEIVED_LIKE;
        
        for (int i = 0; i < post.getLikeCount(); i++) {
            expService.revokeExp(author.getId(), likeReason, "활동 취소에 따른 좋아요 경험치 회수");
        }
        if (post.getLikeCount() > 0) {
            // 게시글 삭제 시 남은 좋아요는 0이 되므로 0, 10 전달하여 전액 회수 유도
            karmaService.revokeKarma(author.getId(), post.getLikeCount(), "POST_ID: " + post.getId(), com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED, 0, 10);
        }
    }

    /**
     * [시니어 조치] 게시글 작성 또는 카테고리 변경 시 활동 보상을 새로 부여함
     */
    @Transactional
    public void grantPostRewards(Post post) {
        User author = post.getAuthor();
        ExpReason postReason = determineExpReason(post.getType(), post.getCategory());
        
        // 1. 게시글 작성 경험치 부여
        expService.grantExp(author.getId(), postReason, "활동 등록(작성/변경): " + post.getId());
        
        // 2. 사진 보너스 부여
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            ExpReason photoReason = ExpReason.SINCERITY_PHOTO_BONUS;
            if (postReason.getCategory() == ExpCategory.KNOWLEDGE) photoReason = ExpReason.KNOWLEDGE_PHOTO_BONUS;
            else if (postReason.getCategory() == ExpCategory.REVIEW) photoReason = ExpReason.REVIEW_PHOTO_BONUS;
            expService.grantExp(author.getId(), photoReason, "활동 등록에 따른 사진 보너스");
        }

        // 3. 좋아요 경험치 및 카르마 부여 (카테고리 변경 시 기존 좋아요 수만큼 재부여)
        // [시니어 조치] 리뷰 분야는 +10, 그 외는 +5 정확히 매핑
        ExpReason likeReason = (postReason.getCategory() == ExpCategory.REVIEW) ? ExpReason.REVIEW_LIKE :
                               (postReason.getCategory() == ExpCategory.KNOWLEDGE) ? ExpReason.KNOWLEDGE_LIKE : ExpReason.RECEIVED_LIKE;
        
        for (int i = 0; i < post.getLikeCount(); i++) {
            expService.grantExp(author.getId(), likeReason, "활동 등록에 따른 좋아요 경험치 재계산");
        }
        
        // [시니어 조치] 사용자 규칙 준수: 게시물당 최대 1.0P(10) 제한 적용
        if (post.getLikeCount() > 0) {
            karmaService.restoreKarma(author.getId(), post.getLikeCount(), "POST_ID: " + post.getId(), null, 10, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
        }
    }

    @Transactional
    public void updatePostTypeAndCategory(Long postId, PostType newType, String newCategory, String newSubCategory) {
        Post post = postRepository.findById(postId).orElseThrow();
        User author = post.getAuthor();

        // [시니어 조치] lazy-loaded 필드 초기화 (LazyInitializationException 방지)
        org.hibernate.Hibernate.initialize(post.getImages());

        // 1. 기존 타입/카테고리 혜택 회수
        revokePostRewards(post);
        
        // 2. 통계 보정 (Review 게시판 출입 시)
        if (post.getType() == PostType.REVIEW) userActivityStatsRepository.decrementReviewPostCount(author.getId());

        // 3. 타입 및 카테고리 실제 업데이트
        post.setType(newType);
        post.setCategory(newCategory);
        post.setSubCategory(newSubCategory);
        
        // 4. 새 타입/카테고리 혜택 부여
        grantPostRewards(post);
        
        // 5. 통계 보정 완료
        if (post.getType() == PostType.REVIEW) userActivityStatsRepository.incrementReviewPostCount(author.getId());
        
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId).orElseThrow();
        // [시니어 조치] 삭제 및 회수 정산 전 상태를 최신화
        entityManager.refresh(post);
        
        if (!post.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("삭제 권한 없음");
        
        // 1. [방어] 이미 삭제된 게시글 중복 처리 방지
        if (!"ACTIVE".equals(post.getStatus())) return;
        
        User author = post.getAuthor();
        
        // 2. [정밀 정산] 실제 DB에 살아있는 댓글 개수 파악 (중복 차감 방지의 핵심)
        long activeCommentCount = post.getComments().stream().filter(c -> "ACTIVE".equals(c.getStatus())).count();
        
        // 3. 활동 통계 원자적 차감
        userActivityStatsRepository.decrementPostCount(author.getId());
        if (post.getType() == PostType.REVIEW) userActivityStatsRepository.decrementReviewPostCount(author.getId());
        if (post.getLikeCount() > 0) userActivityStatsRepository.decrementPostLikeReceivedCountBy(author.getId(), post.getLikeCount());

        // 4. 게시글 보상 회수
        revokePostRewards(post);

        // 5. [물물교환] 게시글 삭제 시 관련 거래 자동 취소
        if (post.getType() == PostType.BARTER) {
            barterService.cancelProposalsByDeletedPost(post);
        }

        // 6. 하위 활동 연쇄 회수 (ACTIVE 상태인 것만 정밀 타격)
        if (post.getComments() != null) {
            post.getComments().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .forEach(comment -> {
                    User cAuthor = comment.getAuthor();
                    // 통계 원자적 차감
                    userActivityStatsRepository.decrementCommentCount(cAuthor.getId());
                    if (comment.getLikeCount() > 0) userActivityStatsRepository.decrementCommentLikeReceivedCountBy(cAuthor.getId(), comment.getLikeCount());
                    // EXP 및 카르마 회수 (이미 보완된 KarmaService 호출)
                    expService.revokeExp(cAuthor.getId(), ExpReason.COMMENT_CREATED, "게시글 삭제 연쇄 회수");
                    for (int i = 0; i < comment.getLikeCount(); i++) {
                        expService.revokeExp(cAuthor.getId(), ExpReason.RECEIVED_LIKE, "댓글 좋아요 회수");
                    }
                    // 카르마 정밀 회수 (게시글 삭제 시 남은 활동은 0이 되므로 0 전달)
                    if (comment.getLikeCount() > 0) {
                        karmaService.revokeKarma(cAuthor.getId(), comment.getLikeCount(), "POST_ID: " + post.getId(), com.habidue.app.domain.user.KarmaReason.LIKE_CANCELED, 0, 10);
                    }
                    comment.changeStatus("USER_DELETED");
                });
        }

        // 6. 상태 변경 및 정리
        post.changeStatus("USER_DELETED");
        post.getPostLikes().clear();
        
        if (post.getNotice() != null) {
            rankingService.increaseNoticeScore(post.getNotice().getId(), -com.habidue.app.service.ranking.RankingService.SCORE_POST);
        }
        postRepository.save(post);
    }
}
