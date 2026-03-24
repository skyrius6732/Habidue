package com.habidue.app.repository.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoticeRepositoryCustom {
    Page<Notice> searchNotices(String keyword, List<String> sources, List<String> statuses, String sortOrder, List<String> userKeywords, User currentUser, boolean showOnlyFuture, Boolean isBoardActive, Boolean isNew, Pageable pageable);

    // QueryDSL 기반 원자적 증감 메서드
    long incrementInterestCount(Long noticeId, int threshold);
    long decrementInterestCount(Long noticeId);

    // QueryDSL 기반 상태 업데이트 메서드
    void updateStatus(Long noticeId, NoticeStatus status);
}
