package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    // 작성자 ID로 게시글 페이징 조회
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    // 조회수 원자적 증가 (전문가적 최적화)
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // 댓글수 원자적 증가 (Atomic update)
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    // 댓글수 원자적 감소
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementCommentCount(@Param("id") Long id);

    // 좋아요수 원자적 증가
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    // 좋아요수 원자적 감소
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    // 이전글 ID 조회 (최신순 기준: 현재 ID보다 큰 첫 번째 ID)
    @Query(value = "SELECT id FROM posts WHERE id > :id AND type = :type AND (notice_id = :noticeId OR (:noticeId IS NULL AND notice_id IS NULL)) ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Long findPrevId(@Param("id") Long id, @Param("type") String type, @Param("noticeId") Long noticeId);

    // 다음글 ID 조회 (최신순 기준: 현재 ID보다 작은 첫 번째 ID)
    @Query(value = "SELECT id FROM posts WHERE id < :id AND type = :type AND (notice_id = :noticeId OR (:noticeId IS NULL AND notice_id IS NULL)) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Long findNextId(@Param("id") Long id, @Param("type") String type, @Param("noticeId") Long noticeId);
    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = 'ACTIVE' AND p.type = :type AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countByKeywordAndType(@org.springframework.data.repository.query.Param("keyword") String keyword, @org.springframework.data.repository.query.Param("type") com.habidue.app.domain.board.PostType type);
}
