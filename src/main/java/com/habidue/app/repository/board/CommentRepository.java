package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    // 특정 게시글의 댓글 목록 조회 (작성자 정보 Fetch Join)
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostIdAndParentIsNull(Long postId, Pageable pageable);

    // 대댓글 목록 조회 (작성자 정보 Fetch Join)
    @EntityGraph(attributePaths = {"author"})
    java.util.List<Comment> findByParentId(Long parentId);

    // 작성자 ID로 댓글 목록 조회 (작성자 정보 Fetch Join)
    @EntityGraph(attributePaths = {"author", "post"})
    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post"})
    Page<Comment> findByAuthorIdAndStatus(Long authorId, String status, Pageable pageable);

    // 게시글 내 모든 ACTIVE 댓글(+답글)의 likeCount 합산 - karma 회수 시 전체 잔여 좋아요 계산용
    @Query("SELECT COALESCE(SUM(c.likeCount), 0) FROM Comment c WHERE c.post.id = :postId AND c.status = 'ACTIVE'")
    Integer sumActiveLikeCountByPostId(@Param("postId") Long postId);

    // 게시글 내 특정 작성자의 ACTIVE 댓글(+답글) likeCount 합산 - 유저별 정밀 카르마 계산용
    @Query("SELECT COALESCE(SUM(c.likeCount), 0) FROM Comment c WHERE c.post.id = :postId AND c.author.id = :authorId AND c.status = 'ACTIVE'")
    Integer sumActiveLikeCountByPostIdAndAuthorId(@Param("postId") Long postId, @Param("authorId") Long authorId);
}
