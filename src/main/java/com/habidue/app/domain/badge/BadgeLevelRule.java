package com.habidue.app.domain.badge;

import jakarta.persistence.*;
import lombok.*;

/**
 * 배지 레벨업 규칙 및 명칭 정보를 관리하는 마스터 테이블
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "badge_level_rules")
public class BadgeLevelRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String badgeType; // KNOWLEDGE, COLLECTOR, REVIEW, COMMUNITY, COMMUNICATOR, ATTENDANCE

    @Column(nullable = false)
    private int level; // 1, 5, 10, 20, 50, 70, 90, 100

    @Column(nullable = false)
    private int requiredValue; // 해당 레벨 달성을 위한 필요 수치

    private String rankEmoji; // 🌱, 🥉 등

    private String rankTitle; // 새싹, 활동가 등

    private String categoryName; // 지식인, 컬렉터 등 (레벨별로 다를 수 있음 - 예: 후기요정 vs 후기작가)

    /**
     * 최종 조합 명칭 반환 (예: 🌱 새싹 지식인)
     */
    public String getFullDisplayName() {
        return rankEmoji + " " + rankTitle + " " + categoryName;
    }

    /**
     * [시니어 조치] 규칙 정보 업데이트
     */
    public void update(int requiredValue, String rankEmoji, String rankTitle, String categoryName) {
        this.requiredValue = requiredValue;
        this.rankEmoji = rankEmoji;
        this.rankTitle = rankTitle;
        this.categoryName = categoryName;
    }
}
