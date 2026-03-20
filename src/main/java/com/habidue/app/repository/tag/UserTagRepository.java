package com.habidue.app.repository.tag;

import com.habidue.app.domain.tag.UserTag;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    List<UserTag> findAllByUser(User user);
    List<UserTag> findByUserId(Long userId);
    Optional<UserTag> findByUserIdAndTagId(Long userId, Long tagId);
    boolean existsByUserIdAndTagId(Long userId, Long tagId);
}
