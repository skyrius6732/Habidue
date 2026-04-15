package com.habidue.app.domain.barter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemCondition {
    NEW("새것"),
    LIKE_NEW("거의 새것"),
    USED("사용감 있음"),
    WORN("흔적 많음");

    private final String description;
}
