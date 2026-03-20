package com.habidue.app.repository.user;

import com.habidue.app.domain.user.UserActivityStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityStatsRepository extends JpaRepository<UserActivityStats, Long> {
}
