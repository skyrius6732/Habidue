package com.habidue.app.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KarmaReason {
    REPORT_APPROVED("신고 승인 (부적절한 게시물/댓글)"),
    FALSE_REPORT("허위 신고 남발 (운영 방해)"),
    MANUAL_ADJUSTMENT("관리자 수동 조정"),
    GOOD_ACTIVITY("모범 활동 포상"),
    LIKE_RECEIVED("게시글 좋아요 획득 (평판 상승)"),
    RESTORATION("제재 복구 및 소명 승인"),
    POST_DELETED("심각한 위반으로 인한 게시글 삭제"),
    COMMENT_DELETED("심각한 위반으로 인한 댓글 삭제");

    private final String description;
}
