package com.habidue.app.dto.notice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.dto.tag.TagResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

import com.habidue.app.domain.tag.NoticeTag;
import com.habidue.app.domain.tag.TagType;

import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class NoticeResponseDto {
    private Long id;
    private String title;
    private String content;

    @JsonProperty("announcementDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime announcementDate;

    @JsonProperty("deadline")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    private String link;
    private String source;
    private String deadlineHint; // 마감일 추출 힌트 문장

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private long interestCount;

    @JsonProperty("isFavorite")
    private boolean isFavorite;

    @JsonProperty("isBoardActive")
    private boolean isBoardActive;

    @JsonProperty("isRevived")
    private boolean isRevived;

    @JsonProperty("isDormant")
    private boolean isDormant; // [시니어 조치] 휴면(보관중) 상태 여부

    private String status;
    
    private String region; // 공고 테이블 '해당지역' 컬럼용 (예: 충주, 전국)
    
    private List<TagResponseDto> tags; // 전체 태그 목록 (객체 형태)

    public NoticeResponseDto(Notice notice) {
        this(notice, List.of(), List.of());
    }

    public NoticeResponseDto(Notice notice, List<NoticeTag> noticeTags, List<String> userKeywords) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.announcementDate = notice.getAnnouncementDate();
        this.deadline = notice.getDeadline();
        this.link = notice.getLink();
        this.source = notice.getSource();
        this.deadlineHint = notice.getDeadlineHint();
        this.createdAt = notice.getCreatedAt();
        this.updatedAt = notice.getUpdatedAt();
        this.interestCount = notice.getInterestCount() != null ? notice.getInterestCount() : 0;
        this.isBoardActive = Boolean.TRUE.equals(notice.getIsBoardActive());
        
        // [시니어 조치] 활성화 상태 판단 (revivedAt 기준 최근 7일)
        this.isRevived = false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (notice.getRevivedAt() != null) {
            this.isRevived = notice.getRevivedAt().isAfter(now.minusDays(7));
        }

        // [시니어 조치] 휴면(보관중) 상태 판별: 활성화된 적이 있지만 7일간 활동이 없고, 아직 깨어나지 않은 경우
        this.isDormant = false;
        if (this.isBoardActive && !this.isRevived) {
            java.time.LocalDateTime lastActivity = notice.getLastPostAt() != null ? notice.getLastPostAt() : notice.getUpdatedAt();
            this.isDormant = lastActivity.isBefore(now.minusDays(7));
        }
        
        // 1. 전체 태그 리스트 생성 (모든 타입 포함: METRO, CITY_COUNTY, TYPE, SPECIAL, TARGET, PROVIDER 등)
        if (noticeTags == null || noticeTags.isEmpty()) {
            this.tags = List.of();
        } else {
            this.tags = noticeTags.stream()
                    .filter(nt -> nt.getTag() != null)
                    .map(nt -> new TagResponseDto(nt.getTag()))
                    .sorted((t1, t2) -> {
                        // 우선순위 1: 사용자 관심 키워드 매칭
                        if (userKeywords != null) {
                            boolean t1Match = userKeywords.contains(t1.getName());
                            boolean t2Match = userKeywords.contains(t2.getName());
                            if (t1Match && !t2Match) return -1;
                            if (!t1Match && t2Match) return 1;
                        }
                        
                        // 우선순위 2: SPECIAL 타입 태그 최상단 배치
                        if (t1.getType() == TagType.SPECIAL && t2.getType() != TagType.SPECIAL) return -1;
                        if (t1.getType() != TagType.SPECIAL && t2.getType() == TagType.SPECIAL) return 1;
                        
                        return 0;
                    })
                    .collect(Collectors.toList());
        }
        
        // 2. 시스템 상태 결정 (엔티티에 저장된 상태가 있다면 우선 사용, 없으면 태그 기반 결정)
        if (notice.getStatus() != null) {
            this.status = notice.getStatus().name();
        } else {
            List<String> tagNames = (this.tags != null) 
                    ? this.tags.stream().map(TagResponseDto::getName).toList()
                    : List.of();
            this.status = determineStatusByTags(tagNames).name();
        }
        
        // 3. 대표 지역 결정
        this.region = determineRepresentativeRegion(noticeTags != null ? noticeTags : List.of());
    }

    /**
     * 태그 데이터를 기반으로 NoticeStatus Enum 값을 결정합니다.
     */
    private NoticeStatus determineStatusByTags(List<String> tags) {
        if (tags != null) {
            if (tags.contains("접수중")) return NoticeStatus.RECRUITING;
            if (tags.contains("결과발표")) return NoticeStatus.RESULT;
            if (tags.contains("마감")) return NoticeStatus.CLOSED;
            if (tags.contains("안내")) return NoticeStatus.INFO;
        }
        String t = this.title;
        if (t.contains("접수") || t.contains("청약")) return NoticeStatus.RECRUITING;
        if (t.contains("발표") || t.contains("결과") || t.contains("당첨")) return NoticeStatus.RESULT;
        return NoticeStatus.INFO;
    }

    /**
     * 분석된 태그 중 가장 구체적인 지역을 선택합니다. (STATION -> CITY_COUNTY -> METRO -> '-')
     */
    private String determineRepresentativeRegion(List<NoticeTag> noticeTags) {
        // 0. 지하철역 (STATION) 태그 검색 (민간임대 가시성 향상)
        Optional<String> station = noticeTags.stream()
                .filter(nt -> nt.getTag().getType() == TagType.STATION)
                .map(nt -> nt.getTag().getName())
                .findFirst();

        if (station.isPresent()) {
            return station.get();
        }

        // 1. 기초 자치단체 (CITY_COUNTY) 태그 검색
        Optional<String> cityCounty = noticeTags.stream()
                .filter(nt -> nt.getTag().getType() == TagType.CITY_COUNTY)
                .map(nt -> nt.getTag().getName())
                .findFirst();

        if (cityCounty.isPresent()) {
            return cityCounty.get().replaceAll("[시군구]$", ""); // 예: 청주시 -> 청주
        }

        // 2. 광역 자치단체 (METRO) 태그 검색
        Optional<String> metro = noticeTags.stream()
                .filter(nt -> nt.getTag().getType() == TagType.METRO)
                .map(nt -> nt.getTag().getName())
                .findFirst();

        if (metro.isPresent()) {
            String name = metro.get();
            // 주요 광역 단체 약칭 처리
            if (name.length() > 2) {
                if (name.endsWith("특별시") || name.endsWith("광역시") || name.endsWith("자치시")) {
                    return name.substring(0, 2); // 서울특별시 -> 서울
                }
                if (name.endsWith("특별자치도")) {
                    return name.substring(0, 2); // 강원특별자치도 -> 강원
                }
                if (name.endsWith("도")) {
                    return name.substring(0, name.length() - 1); // 경기도 -> 경기
                }
            }
            return name;
        }

        // 3. 지역 정보가 없으면 '-' 표시
        return "-";
    }
}
