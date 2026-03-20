package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);
}
