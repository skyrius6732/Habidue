package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Comment;
import com.habidue.app.domain.board.QComment;
import com.habidue.app.domain.board.QPost;
import com.habidue.app.domain.user.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Comment> findComments(Long userId, Long commentId, String keyword, String status, Pageable pageable) {
        QComment comment = QComment.comment;
        QUser author = QUser.user;
        QPost post = QPost.post;

        BooleanBuilder builder = new BooleanBuilder();

        // [시니어 조치] 특정 댓글 ID 직접 조회 필터 (최우선)
        if (commentId != null) {
            builder.and(comment.id.eq(commentId));
        }

        // 1. 사용자 ID 필터
        if (userId != null) {
            builder.and(comment.author.id.eq(userId));
        }

        // 2. 검색어 필터 (댓글 내용 또는 작성자 닉네임)
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(comment.content.containsIgnoreCase(keyword)
                    .or(author.nickname.containsIgnoreCase(keyword)));
        }

        // 3. 상태 필터
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            builder.and(comment.status.eq(status));
        }

        // 4. 메인 쿼리 (Fetch Join 적용하여 N+1 방지)
        JPAQuery<Comment> query = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.author, author).fetchJoin() // 작성자 정보
                .leftJoin(comment.post, post).fetchJoin()     // 게시글 정보
                .where(builder)
                .orderBy(comment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<Comment> content = query.fetch();

        // 5. 전체 카운트 쿼리 (조인 추가)
        Long total = queryFactory
                .select(comment.count())
                .from(comment)
                .leftJoin(comment.author, author)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
