package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostLike;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    boolean existsByPostAndUser(Post post, User user);
    void deleteByPostAndUser(Post post, User user);
}
