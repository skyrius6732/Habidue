package com.habidue.app.domain.notice;

import com.habidue.app.domain.tag.TagType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_keyword_metadata")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class NoticeKeywordMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keyword; // 매칭할 한글 키워드 (예: "강남역", "충주시")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType tagType; // 태그 타입 (STATION, CITY_COUNTY, SUBWAY_LINE 등)

    @Enumerated(EnumType.STRING)
    private NoticeStatus targetStatus; // 시스템 상태 연동용 (필요시 사용)

    // 대표 명칭 (예: "강남역" -> "강남") - 필요시 활용
    @Column
    private String representativeName;
}
