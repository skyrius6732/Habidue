package com.habidue.app.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    LIKE("❤️", "새 좋아요"),
    COMMENT("💬", "새 댓글"),
    REPLY("↪️", "새 답글"),
    MESSAGE("💌", "새 쪽지"),
    NOTICE_KEYWORD("🔍", "키워드 공고"),
    NOTICE_DEADLINE("⏰", "마감 임박"),
    NOTICE_STATUS_CHANGE("🔄", "상태 변경"),
    KARMA_CHANGE("⚖️", "점수 변동"),
    LEVEL_UP("🎉", "레벨 상승"),
    EFFECT_ACQUIRED("✨", "이펙트 획득"),
    BARTER_PROPOSAL("🤝", "교환 제안"),
    PROPOSAL_ACCEPTED("✅", "제안 수락"),
    PROPOSAL_REJECTED("❌", "제안 거절"),
    TRADE_CONDITION_PROPOSED("⚙️", "거래 조건 제시"),
    TRADE_CONDITION_ACCEPTED("✅", "거래 조건 수락"),
    TRADE_CONDITION_COUNTER("♻️", "거래 조건 반박"),
    BARTER_COMPLETED("🎉", "거래 완료"),
    TRADE_CANCELLED("🚫", "거래 취소"),
    SYSTEM("📢", "시스템 알림");

    private final String icon;
    private final String description;
}
