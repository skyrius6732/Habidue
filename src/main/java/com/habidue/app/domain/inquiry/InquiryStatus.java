package com.habidue.app.domain.inquiry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    RECEIVED("접수됨"),
    REVIEWING("검토중"),
    ANSWERED("답변완료"),
    COMPLETED("종료됨");

    private final String description;
}
