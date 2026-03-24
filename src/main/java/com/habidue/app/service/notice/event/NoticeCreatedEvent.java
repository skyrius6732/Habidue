package com.habidue.app.service.notice.event;

import com.habidue.app.domain.notice.Notice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 새 공고가 생성되었을 때 발행되는 이벤트
 */
@Getter
@RequiredArgsConstructor
public class NoticeCreatedEvent {
    private final Notice notice;
}
