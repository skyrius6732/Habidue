package com.habidue.app.domain.barter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProposalStatus {
    PROPOSED("제안됨"),
    NEGOTIATING("협의중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    CANCELLED("취소됨"),
    COMPLETED("완료됨");

    private final String description;
}
