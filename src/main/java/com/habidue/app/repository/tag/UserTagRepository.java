package com.habidue.app.repository.tag;

import com.habidue.app.domain.tag.UserTag;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    
    // [시니어 조치] OSIV 비활성화 대응을 위한 Fetch Join 적용
    @Query("SELECT ut FROM UserTag ut JOIN FETCH ut.tag WHERE ut.user = :user")
    List<UserTag> findAllByUserWithTag(@Param("user") User user);

    List<UserTag> findAllByUser(User user);
    
    // [시니어 조치] UserTagService에서 요구하는 메서드들 복구
    List<UserTag> findByUserId(Long userId);
    
    boolean existsByUserIdAndTagId(Long userId, Long tagId);
    
    Optional<UserTag> findByUserIdAndTagId(Long userId, Long tagId);
    
    void deleteByUserAndTag_Id(User user, Long tagId);
}
