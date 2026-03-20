package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeRepositoryCustom {
    Optional<Notice> findByLink(String link);

    /**
     * [시니어 조치] 휴면 대상 공고 조회: 
     * 1. 활성 상태이고
     * 2. 최근 7일 이내에 깨어난 기록이 없어야 하며 (revivedAt 보호)
     * 3. (마지막 게시글이 30일 전이거나, 게시글이 없는데 공고 생성일이 30일 전인 경우)
     */
    @Query("SELECT n FROM Notice n WHERE n.isBoardActive = true AND " +
           "(n.revivedAt IS NULL OR n.revivedAt < :revivedThreshold) AND (" +
           "(n.lastPostAt IS NOT NULL AND n.lastPostAt < :dormantThreshold) OR " +
           "(n.lastPostAt IS NULL AND n.createdAt < :dormantThreshold))")
    List<Notice> findDormantCandidates(
        @Param("revivedThreshold") java.time.LocalDateTime revivedThreshold,
        @Param("dormantThreshold") java.time.LocalDateTime dormantThreshold
    );

    Optional<Notice> findBySourceAndApiId(String source, String apiId);

    // 통계용
    @Query("SELECT n.source, COUNT(n) FROM Notice n GROUP BY n.source")
    List<Object[]> getCountBySource();

    @Query("SELECT n.status, COUNT(n) FROM Notice n GROUP BY n.status")
    List<Object[]> getCountByStatus();

    @Query("SELECT COUNT(n) FROM Notice n WHERE n.createdAt >= :startDate")
    long countRecent(@Param("startDate") LocalDateTime startDate);

    long countByStatus(NoticeStatus status);
}
