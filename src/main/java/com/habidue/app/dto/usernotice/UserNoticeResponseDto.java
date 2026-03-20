package com.habidue.app.dto.usernotice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.tag.TagResponseDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserNoticeResponseDto {
    private Long id;
    private Long userId;
    private Long noticeId;
    private String noticeTitle;
    private String noticeLink;
    private String noticeSource;
    private String noticeStatus; // 원본 공고 상태
    private List<TagResponseDto> noticeTags; // 원본 공고 태그
    private long interestCount;

    @JsonProperty("isBoardActive")
    private boolean isBoardActive;

    private LocalDateTime noticeAnnouncementDate;
    private LocalDateTime noticeDeadline;
    private LocalDateTime noticeResultDate;
    private String memo;
    private String referenceUrls;
    private LocalDateTime userDeadline; // 유저 개인 설정 마감일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserNoticeResponseDto(UserNotice userNotice) {
        this.id = userNotice.getId();
        this.userId = userNotice.getUser().getId();
        this.noticeId = userNotice.getNotice().getId();
        this.noticeTitle = userNotice.getNotice().getTitle();
        this.noticeLink = userNotice.getNotice().getLink();
        this.noticeSource = userNotice.getNotice().getSource();
        this.noticeStatus = userNotice.getNotice().getStatus().name();
        this.interestCount = userNotice.getNotice().getInterestCount() != null ? userNotice.getNotice().getInterestCount() : 0;
        
        // [시니어 조치] 필드값 또는 실시간 카운트(10명 이상) 기반으로 해금 상태 결정
        this.isBoardActive = Boolean.TRUE.equals(userNotice.getNotice().getIsBoardActive()) || this.interestCount >= 10;
        
        this.noticeTags = userNotice.getNotice().getNoticeTags().stream()
                .map(nt -> new TagResponseDto(nt.getTag()))
                .collect(Collectors.toList());
        this.noticeAnnouncementDate = userNotice.getNotice().getAnnouncementDate();
        this.noticeDeadline = userNotice.getNotice().getDeadline();
        this.noticeResultDate = userNotice.getNotice().getResultDate();
        this.memo = userNotice.getMemo();
        this.referenceUrls = userNotice.getReferenceUrls();
        this.userDeadline = userNotice.getUserDeadline();
        this.createdAt = userNotice.getCreatedAt();
        this.updatedAt = userNotice.getUpdatedAt();
    }
}
