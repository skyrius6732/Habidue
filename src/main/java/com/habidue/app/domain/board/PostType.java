package com.habidue.app.domain.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostType {
    GENERAL("통합광장"),
    NOTICE("공고게시판"),
    REVIEW("당첨후기"),
    PARTNER("하비 파트너스"); // 이사/청소 등 생활 서비스

    private final String description;
}
