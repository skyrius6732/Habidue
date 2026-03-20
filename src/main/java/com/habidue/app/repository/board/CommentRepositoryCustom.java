package com.habidue.app.repository.board;

import com.habidue.app.domain.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepositoryCustom {
    Page<Comment> findComments(Long userId, String keyword, String status, Pageable pageable);
}
