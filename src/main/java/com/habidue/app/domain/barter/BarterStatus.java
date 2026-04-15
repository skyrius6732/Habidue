package com.habidue.app.domain.barter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BarterStatus {
    TRADING("교환 가능"),
    RESERVED("예약 중"),
    COMPLETED("교환 완료");

    private final String description;
}
