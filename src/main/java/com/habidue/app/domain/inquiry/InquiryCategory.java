package com.habidue.app.domain.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryCategory {
    BUG_REPORT("🐛 서비스 버그/오류"),
    FEATURE_SUGGESTION("💡 기능 제안"),
    DATA_CORRECTION("🏠 주거 정보 수정"),
    ACCOUNT_INQUIRY("🔑 계정/인증 문의"),
    PARTNERSHIP("🤝 제휴/광고 문의"),
    OTHER("❓ 기타 문의");

    private final String description;
}
