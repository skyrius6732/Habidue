package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    /**
     * [시니어 조치] MultipleBagFetchException 해결을 위한 최적화된 상세 조회
     * ToOne 관계인 author만 페치 조인으로 즉시 가져오고, 
     * 나머지 컬렉션(images, tags)은 엔티티의 @BatchSize 설정을 통해 효율적으로 지연 로딩함.
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
    Optional<Post> findWithAuthorById(@Param("id") Long id);

    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    Page<Post> findByAuthorIdAndStatus(Long authorId, String status, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    // 게시글 좋아요 수 직접 조회 (JPQL → DB 직접 읽기, @Modifying 후에도 정확한 값 반환)
    @Query("SELECT COALESCE(p.likeCount, 0) FROM Post p WHERE p.id = :id")
    Integer getPostLikeCount(@Param("id") Long id);

    // 특정 작성자의 게시글인 경우에만 좋아요 수 반환 (유저별 정밀 카르마 계산용)
    @Query("SELECT COALESCE(p.likeCount, 0) FROM Post p WHERE p.id = :id AND p.author.id = :authorId")
    Integer getPostLikeCountForAuthor(@Param("id") Long id, @Param("authorId") Long authorId);

    @Query(value = "SELECT id FROM posts WHERE id > :id AND type = :type AND (notice_id = :noticeId OR (:noticeId IS NULL AND notice_id IS NULL)) ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Long findPrevId(@Param("id") Long id, @Param("type") String type, @Param("noticeId") Long noticeId);

    @Query(value = "SELECT id FROM posts WHERE id < :id AND type = :type AND (notice_id = :noticeId OR (:noticeId IS NULL AND notice_id IS NULL)) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Long findNextId(@Param("id") Long id, @Param("type") String type, @Param("noticeId") Long noticeId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = 'ACTIVE' AND p.type = :type AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countByKeywordAndType(@Param("keyword") String keyword, @Param("type") com.habidue.app.domain.board.PostType type);

    // [시니어 조치] 누락된 메서드 복구 (OSIV OFF 대응 Fetch Join 추가)
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.notice.id = :noticeId AND p.status = 'ACTIVE' ORDER BY (p.viewCount + p.likeCount * 5) DESC")
    List<Post> findTopByNoticeIdOrderByPopularity(@Param("noticeId") Long noticeId, Pageable pageable);

    // 대시보드용 게시글 상태별 건수
    @Query("SELECT p.status, COUNT(p) FROM Post p GROUP BY p.status")
    List<Object[]> getCountByPostStatus();
}
