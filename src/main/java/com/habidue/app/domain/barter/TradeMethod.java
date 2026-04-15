package com.habidue.app.domain.barter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeMethod {
    DIRECT("직거래"),
    DOORSTEP("문고리 거래"),
    PARCEL("택배 교환");

    private final String description;
}
