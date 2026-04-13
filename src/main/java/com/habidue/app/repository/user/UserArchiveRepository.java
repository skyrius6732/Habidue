package com.habidue.app.repository.user;

import com.habidue.app.domain.user.UserArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserArchiveRepository extends JpaRepository<UserArchive, Long> {
    Optional<UserArchive> findByTargetUserId(Long targetUserId);

    /**
     * 특정 시각 이전에 생성된 아카이브 데이터를 일괄 삭제합니다.
     * @param threshold 기준 시각
     * @return 삭제된 레코드 수
     */
    int deleteByWithdrawnAtBefore(LocalDateTime threshold);
}
