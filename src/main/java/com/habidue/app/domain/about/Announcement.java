package com.habidue.app.domain.about;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "announcements")
public class Announcement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String tag; // 중요, 안내 등

    @Column(length = 20)
    private String type; // notice, info 등

    @Column(length = 20)
    private String date; // 표시용 날짜 (예: 2026.03.09)

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    public void update(String tag, String type, String date, String title, String content) {
        this.tag = tag;
        this.type = type;
        this.date = date;
        this.title = title;
        this.content = content;
    }
}
