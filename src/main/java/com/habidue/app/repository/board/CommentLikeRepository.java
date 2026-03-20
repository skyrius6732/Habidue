package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Comment;
import com.habidue.app.domain.board.CommentLike;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);
    boolean existsByCommentAndUser(Comment comment, User user);
}
