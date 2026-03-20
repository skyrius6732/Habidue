package com.habidue.app.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("정상"),
    RESTRICTED("활동 제한"), // 카르마 기반 정지
    BLOCKED("차단됨"),
    WITHDRAWN("탈퇴함");

    private final String description;
}
