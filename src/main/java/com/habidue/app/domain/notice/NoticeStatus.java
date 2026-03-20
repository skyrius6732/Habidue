package com.habidue.app.domain.notice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공고의 논리적 상태 정의
 */
@Getter
@RequiredArgsConstructor
public enum NoticeStatus {
    // 순서: 만료 시 전이될 상태(name), 우선순위(높을수록 강함), 설명
    RESULT_COMPLETED(null, 3, "발표완료"),
    RESULT("RESULT_COMPLETED", 3, "결과발표"),
    CLOSED(null, 2, "마감"),
    RECRUITING("CLOSED", 2, "접수중"),
    EXPIRED_INFO(null, 1, "이전안내"),
    INFO("EXPIRED_INFO", 1, "안내"),
    UPCOMING(null, 0, "예정"),
    RECRUITING_NON_STOP(null, 2, "원문확인");

    private final String expiredStatusName;
    private final int priority;
    private final String description;

    public NoticeStatus getExpired() {
        if (this.expiredStatusName == null) return this;
        return NoticeStatus.valueOf(this.expiredStatusName);
    }
}
