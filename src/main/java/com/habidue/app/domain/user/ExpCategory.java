package com.habidue.app.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpCategory {
    KNOWLEDGE("지식공유"), 
    REVIEW("리뷰활동"), 
    SINCERITY("성실활동"), 
    ADMIN("관리자보정");

    private final String description;
}