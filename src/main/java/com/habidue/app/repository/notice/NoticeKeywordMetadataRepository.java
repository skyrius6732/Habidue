package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.NoticeKeywordMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeKeywordMetadataRepository extends JpaRepository<NoticeKeywordMetadata, Long> {
    // 모든 키워드 조회 (분석용 캐싱을 위해 사용)
    List<NoticeKeywordMetadata> findAll();
}
