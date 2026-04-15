package com.habidue.app.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KarmaReason {
    REPORT_APPROVED("신고 승인 (부적절한 게시물/댓글)"),
    REPORT_POST_APPROVED("게시글 신고 승인 (부적절한 게시물)"),
    REPORT_COMMENT_APPROVED("댓글 신고 승인 (부적절한 댓글)"),
    REPORT_MESSAGE_APPROVED("쪽지 신고 승인 (부적절한 메시지)"),
    FALSE_REPORT("허위 신고 남발 (운영 방해)"),
    MANUAL_ADJUSTMENT("관리자 수동 조정"),
    GOOD_ACTIVITY("모범 활동 포상"),
    LIKE_RECEIVED("게시글/댓글 좋아요 획득 (신뢰 상승)"),
    LIKE_CANCELED("좋아요 취소에 따른 신뢰 점수 회수"),
    RESTORATION("제재 복구 및 소명 승인"),
    ADMIN_REVERSAL("관리자 조치 번복 (점수 복구)"),
    POST_DELETED("심각한 위반으로 인한 게시글 삭제"),
    COMMENT_DELETED("심각한 위반으로 인한 댓글 삭제"),
    MESSAGE_SENT("쪽지 발송 (카르마 소모)"),
    BARTER_PROPOSAL("물물교환 제안 (카르마 소모)"),
    BARTER_COMPLETED("물물교환 거래 성공 (신뢰 상승)"),
    BARTER_CANCELLED("물물교환 거래 취소 (신뢰 감점)");

    private final String description;
}
