package com.habidue.app.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    COMMENT("💬", "새 댓글"),
    REPLY("↪️", "새 답글"),
    MESSAGE("💌", "새 쪽지"),
    NOTICE_KEYWORD("🔍", "키워드 공고"),
    NOTICE_DEADLINE("⏰", "마감 임박"),
    NOTICE_STATUS_CHANGE("🔄", "상태 변경"),
    KARMA_CHANGE("⚖️", "점수 변동"),
    SYSTEM("📢", "시스템 알림");

    private final String icon;
    private final String description;
}
