package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import com.habidue.app.domain.board.QPost;
import com.habidue.app.domain.user.QUser;
import com.habidue.app.domain.notice.QNotice;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findPosts(PostType type, Long noticeId, String category, String subCategory, String keyword, String status, String regionTag, String tagName, Pageable pageable, Long currentUserId, Long targetPostId, boolean isAdmin) {
        QPost post = QPost.post;
        QUser author = QUser.user;
        QNotice notice = QNotice.notice;

        BooleanBuilder builder = new BooleanBuilder();

        // [시니어 조치] 특정 ID 직접 조회 필터 (최우선)
        if (targetPostId != null && isAdmin) {
            builder.and(post.id.eq(targetPostId));
        }

        // 1. 게시글 타입, 공고, 카테고리 필터
        if (type != null) {
            builder.and(post.type.eq(type));
        }
        if (noticeId != null) {
            builder.and(post.notice.id.eq(noticeId));
        }

        // [핵심 로직] 소메뉴(게시판 위치) 필터링
        if (subCategory != null && !subCategory.trim().isEmpty() && !"ALL".equalsIgnoreCase(subCategory)) {
            builder.and(post.subCategory.eq(subCategory));
        }

        // 소통 주제(Flair) 필터
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            builder.and(post.category.eq(category));
        }

        // 2. 지역(regionTag) 필터 (레거시 지원 및 명예시민용)
        if (regionTag != null && !regionTag.trim().isEmpty()) {
            builder.and(post.regionTag.containsIgnoreCase(regionTag));
        }

        // 3. 특정 태그 명칭(tagName) 필터 (예: 단지명 검색)
        if (tagName != null && !tagName.trim().isEmpty()) {
            builder.and(post.tags.any().tag.name.eq(tagName));
        }

        // 4. [시니어 조치] 상태 필터 (관리자 전체 노출 및 작성자 본인 노출 로직 포함)
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                if (isAdmin) {
                    // 관리자는 ACTIVE 요청 시에도 BLINDED와 DELETED를 모두 모니터링 가능
                    builder.and(post.status.ne("HARD_DELETED")); // 진짜 지운 것만 빼고 다 봄
                } else if (currentUserId != null) {
                    // 일반 유저는 본인 것 중 BLINDED는 보되, DELETED는 본인이라도 절대 못 봄
                    builder.and(post.status.eq("ACTIVE")
                            .or(post.status.eq("BLINDED").and(post.author.id.eq(currentUserId))));
                } else {
                    builder.and(post.status.eq("ACTIVE"));
                }
            } else {
                builder.and(post.status.eq(status));
            }
        } else {
            // 기본(status == null or ALL) 조회 시
            if (isAdmin) {
                builder.and(post.status.ne("HARD_DELETED"));
            } else if (currentUserId != null) {
                // 일반 유저는 본인 글(ACTIVE + BLINDED)만 조회 가능 (삭제 글 제외)
                builder.and(post.status.eq("ACTIVE")
                        .or(post.status.eq("BLINDED").and(post.author.id.eq(currentUserId))));
            } else {
                builder.and(post.status.eq("ACTIVE"));
            }
        }

        // 5. 검색어 필터 (제목, 내용, 작성자 닉네임 또는 태그 이름)
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword))
                    .or(author.nickname.containsIgnoreCase(keyword))
                    .or(post.tags.any().tag.name.containsIgnoreCase(keyword)));
        }

        // 6. 메인 쿼리 (Fetch Join 적용하여 N+1 방지)
        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .leftJoin(post.author, author).fetchJoin() // 작성자 정보 미리 로딩
                .leftJoin(post.notice, notice).fetchJoin() // 공고 정보 미리 로딩
                .where(builder)
                .orderBy(post.createdAt.desc()) // 최신순 기본 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Post> content = query.fetch();

        // 7. 전체 카운트 쿼리
        Long total = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.author, author) // 검색 조건에 nickname이 포함되므로 조인 필수
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Post> findPostsByAuthor(Long authorId, String keyword, String status, Pageable pageable) {
        QPost post = QPost.post;
        QUser author = QUser.user;
        QNotice notice = QNotice.notice;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(post.author.id.eq(authorId));

        if (status != null && !status.trim().isEmpty()) {
            builder.and(post.status.eq(status));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword)));
        }

        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .leftJoin(post.author, author).fetchJoin()
                .leftJoin(post.notice, notice).fetchJoin()
                .where(builder)
                .orderBy(post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Post> content = query.fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<Post> findTopByNoticeIdOrderByPopularity(Long noticeId, int limit) {
        QPost post = QPost.post;
        QUser author = QUser.user;

        // [시니어 가중치 로직] 조회수와 댓글수를 합산하여 상위 3개 추출
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.author, author).fetchJoin()
                .where(post.notice.id.eq(noticeId))
                .orderBy(post.viewCount.add(post.commentCount.multiply(3)).desc(), post.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Post> findTrendingPosts(int limit) {
        QPost post = QPost.post;
        QUser author = QUser.user;
        // [시니어 조치] '최근 24시간' 대신 '오늘(00:00:00부터 현재까지)'로 기준 변경
        java.time.LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();

        // [시니어 가중치 알고리즘] 오늘 게시된 글 중 인기글 추출
        // 점수 = (조회수 * 1) + (좋아요 * 5) + (댓글수 * 10)
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.author, author).fetchJoin()
                .where(post.createdAt.after(todayStart)
                        .and(post.status.eq("ACTIVE")))
                .orderBy(
                    post.viewCount
                        .add(post.likeCount.multiply(5))
                        .add(post.commentCount.multiply(10))
                        .desc(),
                    post.createdAt.desc()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByKeywordAndType(String keyword, PostType type, Long targetPostId, boolean isAdmin) {
        QPost post = QPost.post;
        QUser author = QUser.user;
        BooleanBuilder builder = new BooleanBuilder();

        if (type != null) {
            builder.and(post.type.eq(type));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(post.title.containsIgnoreCase(keyword)
                    .or(post.content.containsIgnoreCase(keyword))
                    .or(author.nickname.containsIgnoreCase(keyword))
                    .or(post.tags.any().tag.name.containsIgnoreCase(keyword)));
        }

        // [시니어 조치] 카운트 쿼리에도 검색 리스트와 동일한 권한 기반 상태 필터 적용
        if (isAdmin) {
            builder.and(post.status.ne("HARD_DELETED"));
        } else if (targetPostId != null) {
            builder.and(post.status.eq("ACTIVE")
                    .or(post.status.eq("BLINDED").and(post.author.id.eq(targetPostId))));
        } else {
            builder.and(post.status.eq("ACTIVE"));
        }

        Long count = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.author, author)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;
    }
}
