package com.habidue.app.dto.ranking;

import com.habidue.app.domain.notice.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HotNoticeResponseDto {
    private Long id;
    private String title;
    private String source;
    private String status;
    private LocalDateTime deadline;
    private Integer interestCount;
    private long postCount; 
    private long viewCount; // [시니어 조치] 순수 조회수 추가
    private double score; 
    private int rank;

    public static HotNoticeResponseDto from(Notice notice, double score, int rank, long viewCount) {
        return HotNoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .source(notice.getSource())
                .status(notice.getStatus().name())
                .deadline(notice.getDeadline())
                .interestCount(notice.getInterestCount())
                .postCount(notice.getPosts() != null ? notice.getPosts().size() : 0)
                .viewCount(viewCount)
                .score(score)
                .rank(rank)
                .build();
    }
}
