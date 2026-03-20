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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    private final ExpService expService; // [시니어 조치] EXP 연동
    private final KarmaService karmaService; // [시니어 조치] 카르마 시스템 연동

    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, UserPrincipal currentUser) {
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // [시니어 페널티 체크] 활동 제한 여부 검증
        if (karmaService.isRestricted(author)) {
            LocalDateTime until = author.getRestrictedUntil();
            String message;
            
            // 50년 이상 제한이면 사실상 영구 제재로 간주
            if (until != null && until.isAfter(LocalDateTime.now().plusYears(50))) {
                message = "카르마 점수가 너무 낮아 커뮤니티에서 영구 활동이 제한되었습니다.\n상세 내용은 고객센터로 문의해 주세요.";
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                message = "카르마 점수가 낮아 커뮤니티 활동이 제한된 상태입니다.\n(제한 종료 시각: " + until.format(formatter) + ")";
            }
            throw new IllegalArgumentException(message);
        }

        Notice notice = null;

        if (requestDto.getType() == PostType.NOTICE) {
            if (requestDto.getNoticeId() == null) {
                throw new IllegalArgumentException("공고 전용 게시판에는 공고 ID가 필수입니다.");
            }
            notice = noticeRepository.findById(requestDto.getNoticeId())
                    .orElseThrow(() -> new NoSuchElementException("해당 공고를 찾을 수 없습니다."));
        }

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .category(requestDto.getCategory())
                .subCategory(requestDto.getSubCategory())
                .regionTag(requestDto.getRegionTag())
                .notice(notice)
                .author(author)
                .build();

        // 1. 이미지 처리
        if (requestDto.getImageUrls() != null) {
            for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder()
                        .post(post)
                        .imageUrl(requestDto.getImageUrls().get(i))
                        .sortOrder(i)
                        .build());
            }
        }

        // 2. 태그 처리 (연관관계 편의 메서드 사용)
        if (requestDto.getTagIds() != null && !requestDto.getTagIds().isEmpty()) {
            List<com.habidue.app.domain.tag.Tag> tags = tagRepository.findAllById(requestDto.getTagIds());
            for (com.habidue.app.domain.tag.Tag tag : tags) {
                post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag));
            }
        }

        Post savedPost = postRepository.save(post);

        // [시니어 조치] 공고 소통방 활동 시각 동기화
        if (savedPost.getNotice() != null) {
            savedPost.getNotice().setLastPostAt(java.time.LocalDateTime.now());
        }

        // [시니어 조치] 유저 활동 통계 업데이트 (지연 초기화 적용 - 영속화된 author 사용)
        UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
        
        stats.incrementPostCount();
        if (savedPost.getType() == PostType.REVIEW) {
            stats.incrementReviewPostCount();
        }
        userActivityStatsRepository.save(stats);
        badgeService.checkAndAwardBadges(stats);
        
        // [시니어 조치] 게시글 주제 및 타입에 따른 최종 EXP 매핑 (가이드 동기화)
        ExpReason reason;
        
        if ("FREE".equalsIgnoreCase(savedPost.getCategory())) {
            // 0순위: 자유수다는 어느 게시판에서든 열정파 (10P)
            reason = ExpReason.POST_CREATED;
        } else if (isKnowledgeCategory(savedPost.getCategory()) || savedPost.getType() == PostType.NOTICE) {
            // 1순위: 지식인 관련 주제 또는 공고 소통방 -> 지식인 (30P)
            reason = ExpReason.KNOWLEDGE_SHARE;
        } else if (isReviewCategory(savedPost.getCategory()) || savedPost.getType() == PostType.REVIEW) {
            // 2순위: 리뷰 관련 주제 또는 당첨후기 게시판 -> 리뷰왕 (40P)
            reason = ExpReason.PRODUCT_REVIEW;
        } else {
            // 3순위: 그 외 일반글 -> 열정파 (10P)
            reason = ExpReason.POST_CREATED;
        }
        
        log.info("[EXP_GRANT] User: {}, Reason: {}, Category: {}, Board: {}", author.getNickname(), reason, savedPost.getCategory(), savedPost.getType());
        expService.grantExp(author.getId(), reason, "게시글 작성: " + savedPost.getId());

        // [시니어 조치] 사진 첨부 시 해당 분야별 추가 보너스 EXP 부여 (+10 EXP)
        if (savedPost.getImages() != null && !savedPost.getImages().isEmpty()) {
            ExpReason photoReason = ExpReason.SINCERITY_PHOTO_BONUS; // 기본
            if (reason == ExpReason.KNOWLEDGE_SHARE) {
                photoReason = ExpReason.KNOWLEDGE_PHOTO_BONUS;
            } else if (reason == ExpReason.PRODUCT_REVIEW) {
                photoReason = ExpReason.REVIEW_PHOTO_BONUS;
            }
            expService.grantExp(author.getId(), photoReason, "사진 첨부 보너스: " + savedPost.getId());
        }

        return PostResponseDto.from(savedPost);
    }

    /**
     * [시니어] 지식 공유 성격의 카테고리인지 판별 (가이드와 100% 일치)
     */
    private boolean isKnowledgeCategory(String category) {
        if (category == null || category.trim().isEmpty()) return false;
        String upperCat = category.toUpperCase().trim();
        List<String> knowledgeCats = java.util.Arrays.asList(
            "INQUIRY", "PREPARE", "PHOTO", "COMPETE", 
            "INST_TIPS", "STATUS", "DOCUMENT", "LOAN", "TIPS", "SECRET"
        );
        return knowledgeCats.contains(upperCat);
    }

    /**
     * [시니어] 리뷰/경험 성격의 카테고리인지 판별
     * SUCCESS_STORY(내집마련기), MOVE_IN(사전점검), PARTNER_REVIEW(입주/업체후기),
     * ESTIMATE(견적공유), CHECKLIST(체크리스트)
     */
    private boolean isReviewCategory(String category) {
        if (category == null || category.trim().isEmpty()) return false;
        String upperCat = category.toUpperCase().trim();
        return java.util.Arrays.asList("SUCCESS_STORY", "MOVE_IN", "PARTNER_REVIEW", "ESTIMATE", "CHECKLIST").contains(upperCat);
    }

    private void fillAuthorBadgeInfo(PostResponseDto dto, User author) {
        List<com.habidue.app.dto.badge.BadgeResponseDto> badgeDtos = userBadgeRepository.findByUserOrderByAcquiredAtDesc(author).stream()
                .map(ub -> {
                    String badgeType = ub.getBadge().getCode().replace("_BASE", "");
                    com.habidue.app.domain.badge.BadgeLevelRule rule = 
                        badgeLevelRuleRepository.findByBadgeTypeAndLevel(badgeType, ub.getLevel());
                    String realName = (rule != null) ? rule.getFullDisplayName() : ub.getBadge().getName();
                    return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, realName);
                })
                .collect(Collectors.toList());
        
        dto.setAuthorBadges(badgeDtos);
        
        // 대표 배지 명칭 찾기
        if (author.getEquippedBadgeId() != null) {
            badgeDtos.stream()
                    .filter(b -> b.getId().equals(author.getEquippedBadgeId()))
                    .findFirst()
                    .ifPresent(b -> dto.setAuthorEquippedBadgeName(b.getDisplayName()));
        }
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPosts(PostType type, Long noticeId, String category, String subCategory, String keyword, String regionTag, String tagName, UserPrincipal currentUser, Pageable pageable) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        Page<Post> posts = postRepository.findPosts(type, noticeId, category, subCategory, keyword, "ACTIVE", regionTag, tagName, pageable, currentUserId, isAdmin);
        
        return posts.map(post -> {
            PostResponseDto dto = PostResponseDto.from(post);
            
            // [시니어 조치] 작성자 배지 정보 및 대표 칭호 추가
            fillAuthorBadgeInfo(dto, post.getAuthor());

            if (currentUser != null) {
                dto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
            }
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopPosts(Long noticeId, int limit) {
        return postRepository.findTopByNoticeIdOrderByPopularity(noticeId, limit).stream()
                .map(post -> {
                    PostResponseDto dto = PostResponseDto.from(post);
                    fillAuthorBadgeInfo(dto, post.getAuthor());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTrendingPosts(int limit) {
        return postRepository.findTrendingPosts(limit).stream()
                .map(post -> {
                    PostResponseDto dto = PostResponseDto.from(post);
                    fillAuthorBadgeInfo(dto, post.getAuthor());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PostResponseDto getPostDetail(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        // [시니어 조치] 관리자가 아닌 경우 삭제된 게시글 접근 차단
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if ("DELETED".equalsIgnoreCase(post.getStatus()) && !isAdmin) {
            throw new NoSuchElementException("게시글을 찾을 수 없습니다.");
        }

        postRepository.incrementViewCount(postId);
        PostResponseDto responseDto = PostResponseDto.from(post);

        // [시니어 조치] 게시글 작성자의 누적 조회수 통계 업데이트 (지연 초기화 적용)
        userActivityStatsRepository.findById(post.getAuthor().getId())
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(post.getAuthor())))
                .incrementViewReceivedCount();

        // [시니어 조치] 작성자 배지 정보 및 대표 칭호 추가
        fillAuthorBadgeInfo(responseDto, post.getAuthor());

        if (currentUser != null) {
            responseDto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
        }
        return responseDto;
    }

    @Transactional
    public PostResponseDto updatePost(Long postId, PostRequestDto requestDto, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        // [시니어 조치] 삭제된 게시글 수정 차단
        if ("DELETED".equalsIgnoreCase(post.getStatus())) {
            throw new IllegalArgumentException("관리자에 의해 삭제된 게시글은 수정할 수 없습니다.");
        }

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        // 1. 기본 정보 수정 (regionTag 포함)
        post.update(requestDto.getTitle(), requestDto.getContent(), requestDto.getCategory(), requestDto.getSubCategory(), requestDto.getRegionTag());

        // 2. 이미지 수정
        post.getImages().clear();
        if (requestDto.getImageUrls() != null) {
            for (int i = 0; i < requestDto.getImageUrls().size(); i++) {
                post.getImages().add(com.habidue.app.domain.board.PostImage.builder()
                        .post(post)
                        .imageUrl(requestDto.getImageUrls().get(i))
                        .sortOrder(i)
                        .build());
            }
        }

        // 3. 태그 수정 (핵심: 편의 메서드로 DB 저장 강제)
        post.clearTags();
        if (requestDto.getTagIds() != null && !requestDto.getTagIds().isEmpty()) {
            List<com.habidue.app.domain.tag.Tag> tags = tagRepository.findAllById(requestDto.getTagIds());
            for (com.habidue.app.domain.tag.Tag tag : tags) {
                post.addPostTag(com.habidue.app.domain.board.PostTag.create(post, tag));
            }
        }

        return PostResponseDto.from(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public com.habidue.app.dto.search.SearchResponseDto searchAll(String keyword, org.springframework.data.domain.Pageable pageable, UserPrincipal currentUser) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return com.habidue.app.dto.search.SearchResponseDto.builder()
                    .keyword(keyword)
                    .totalCount(0)
                    .resultsByCategory(new java.util.HashMap<>())
                    .build();
        }

        // 1. 요청된 페이징에 맞춰 게시글 검색 (10개씩, 최신순 정렬 강제)
        org.springframework.data.domain.Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );
        boolean isAdmin = currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        org.springframework.data.domain.Page<Post> postsPage = postRepository.findPosts(null, null, null, null, keyword, "ACTIVE", null, null, sortedPageable, currentUser != null ? currentUser.getId() : null, isAdmin);
        List<Post> pagedPosts = postsPage.getContent();

        // [시니어 조치] 카테고리별 전체 개수 카운트
        java.util.Map<String, Long> categoryCounts = new java.util.HashMap<>();
        for (PostType type : PostType.values()) {
            categoryCounts.put(type.name(), postRepository.countByKeywordAndType(keyword, type));
        }

        // 2. 검색 점수 계산 및 Snippet 추출하여 DTO 변환
        List<com.habidue.app.dto.search.SearchResponseDto.SearchResultItem> items = pagedPosts.stream()
                .map(post -> {
                    double score = calculateSearchScore(post, keyword);
                    String snippet = createSnippet(post.getContent(), keyword);
                    PostResponseDto dto = PostResponseDto.from(post);
                    
                    // [시니어 조치] 통합 검색 결과에도 작성자 배지 정보 및 대표 칭호 추가
                    fillAuthorBadgeInfo(dto, post.getAuthor());

                    if (currentUser != null) {
                        dto.setLiked(postLikeRepository.existsByPostAndUser(post, User.builder().id(currentUser.getId()).build()));
                    }
                    return com.habidue.app.dto.search.SearchResponseDto.SearchResultItem.builder()
                            .post(dto)
                            .snippet(snippet)
                            .searchScore(score)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getSearchScore(), a.getSearchScore())) // 페이지 내 고득점순 정렬
                .collect(Collectors.toList());

        // 3. 카테고리(PostType)별로 그룹화
        java.util.Map<String, List<com.habidue.app.dto.search.SearchResponseDto.SearchResultItem>> groupedResults = items.stream()
                .collect(Collectors.groupingBy(item -> item.getPost().getType().name()));

        return com.habidue.app.dto.search.SearchResponseDto.builder()
                .keyword(keyword)
                .totalCount((int) postsPage.getTotalElements())
                .categoryCounts(categoryCounts) // [시니어 조치] 계산된 전체 개수 맵 추가
                .resultsByCategory(groupedResults)
                .build();
    }

    /**
     * 검색 가중치 계산 (시니어 엔진)
     */
    private double calculateSearchScore(Post post, String keyword) {
        double score = 0;
        String lowerKeyword = keyword.toLowerCase();

        // 제목 일치 (가장 높음)
        if (post.getTitle().toLowerCase().contains(lowerKeyword)) {
            score += 10.0;
            if (post.getTitle().equalsIgnoreCase(keyword)) score += 5.0; // 완전 일치 보너스
        }

        // 본문 일치
        if (post.getContent().toLowerCase().contains(lowerKeyword)) {
            score += 5.0;
        }

        // 태그 일치
        boolean tagMatch = post.getTags().stream()
                .anyMatch(pt -> pt.getTag().getName().toLowerCase().contains(lowerKeyword));
        if (tagMatch) score += 7.0;

        // 인기도 가중치 (조회수, 좋아요 등)
        score += (post.getViewCount() * 0.01);
        score += (post.getLikeCount() * 0.1);

        return score;
    }

    /**
     * 본문에서 검색어가 포함된 부분을 추출 (Snippet)
     */
    private String createSnippet(String content, String keyword) {
        if (content == null || keyword == null) return "";
        
        int index = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) {
            return content.length() > 150 ? content.substring(0, 150) + "..." : content;
        }

        int start = Math.max(0, index - 50);
        int end = Math.min(content.length(), index + 100);
        
        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        
        return snippet;
    }

    @Transactional(readOnly = true)
    public List<com.habidue.app.dto.tag.TagResponseDto> getPopularKeywords(int limit) {
        return tagService.getPopularTags(limit);
    }

    @Transactional
    public void deletePost(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        if (!post.getAuthor().getId().equals(currentUser.getId())) throw new IllegalArgumentException("삭제 권한이 없습니다.");
        
        postRepository.delete(post);

        // [시니어 조치] 유저 활동 통계 업데이트 (글 삭제)
        userActivityStatsRepository.findById(currentUser.getId()).ifPresent(stats -> {
            stats.decrementPostCount();
            if (post.getType() == PostType.REVIEW) {
                stats.decrementReviewPostCount();
            }
            userActivityStatsRepository.save(stats);
        });
    }

    @Transactional
    public boolean toggleLike(Long postId, UserPrincipal currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        User user = User.builder().id(currentUser.getId()).build();
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

        // [시니어 조치] 게시글 작성자의 받은 좋아요 통계 업데이트 (지연 초기화 적용)
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            // post.getAuthor()는 이미 영속 상태이므로 그대로 사용
            User author = post.getAuthor();
            UserActivityStats stats = userActivityStatsRepository.findById(author.getId())
                    .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(author)));
            
            if (isNowLiked) {
                stats.incrementPostLikeReceivedCount();
                
                // [시니어 조치] 게시글 주제 및 타입에 따른 최종 하트 점수 매핑 (가이드 동기화)
                ExpReason likeReason = ExpReason.RECEIVED_LIKE; // 기본: 열정파 (5P)
                
                if ("FREE".equalsIgnoreCase(post.getCategory())) {
                    likeReason = ExpReason.RECEIVED_LIKE; // 자유수다는 항상 일반 하트 (5P)
                } else if (isKnowledgeCategory(post.getCategory()) || post.getType() == PostType.NOTICE) {
                    likeReason = ExpReason.KNOWLEDGE_LIKE; // 지식 하트 (5P)
                } else if (isReviewCategory(post.getCategory()) || post.getType() == PostType.REVIEW) {
                    likeReason = ExpReason.REVIEW_LIKE; // 리뷰 하트 (10P)
                }

                log.info("[LIKE_EXP] User: {}, Reason: {}, Category: {}", post.getAuthor().getNickname(), likeReason, post.getCategory());
                expService.grantExp(author.getId(), likeReason, "게시글 좋아요 수신: " + postId);
                
                // [운영 정책 반영] 게시글당 최대 10점(100포인트)까지만 카르마 획득 가능하도록 설정
                String postKey = "POST_ID: " + postId;
                karmaService.restoreKarma(author.getId(), 10, postKey, null, 100, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED);
            } else {
                stats.decrementPostLikeReceivedCount();
                // [시니어] 좋아요 취소 시 카르마 1점(10포인트) 차감 (어뷰징 방지, 단 제재 트리거는 하지 않음)
                karmaService.manualAdjustKarma(author.getId(), -1, com.habidue.app.domain.user.KarmaReason.LIKE_RECEIVED, "게시글 좋아요 취소 (ID: " + postId + ")", null, false);
            }
            
            userActivityStatsRepository.save(stats);
            badgeService.checkAndAwardBadges(stats);
        }

        return isNowLiked;
    }
}

