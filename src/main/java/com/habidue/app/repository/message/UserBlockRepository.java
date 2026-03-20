package com.habidue.app.repository.message;

import com.habidue.app.domain.message.UserBlock;
import com.habidue.app.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    
    // 차단 여부 확인
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    // 차단 목록 조회
    List<UserBlock> findByBlocker(User blocker);

    // 차단 해제용
    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);
}
