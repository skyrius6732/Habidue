package com.habidue.app.domain.badge;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 커뮤니티 활동 배지 메타데이터 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "badges")
public class Badge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // 예: KNOWLEDGE_1, COLLECTOR_1

    @Column(nullable = false)
    private String name; // 예: 주택 지식인

    @Column(nullable = false)
    private String description; // 예: 양질의 답변으로 커뮤니티에 기여한 유저

    private String iconUrl; // 배지 이미지 아이콘 (추후 확장용, 초기엔 이모지 등 텍스트 활용 가능)

    @Column(nullable = false)
    private String type; // INFO, SINCERITY, REGION, REVIEW 등

    private int level; // 배지 레벨 (1, 2, 3...)

    @Column(length = 500)
    private String badgeTip; // [시니어 조치] 레벨업을 위한 가이드 팁 추가
}
