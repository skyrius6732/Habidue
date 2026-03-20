package com.habidue.app.domain.notice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.habidue.app.domain.tag.NoticeTag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"source", "apiId"}))
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source; // 크롤링 출처 (예: LH, SH)

    @Column
    private String apiId; // 각 기관의 고유 ID (seq, panId 등)

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime announcementDate; // 공고 게시일

    private LocalDateTime deadline; // 공고 마감일 (확정된 경우만)

    private LocalDateTime resultDate; // 당첨자/결과 발표일

    @Column(length = 1000)
    private String deadlineHint; // 마감일 추출 힌트 문장

    @Column(nullable = false, unique = true)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private NoticeStatus status = NoticeStatus.INFO;

    @Column(nullable = false)
    @Builder.Default
    private Integer interestCount = 0; // 관심(찜) 등록 유저 수

    @Column(nullable = false)
    @Builder.Default
    private Boolean isBoardActive = false; // 게시판 활성화 여부 (임계치 도달 시 true)

    private LocalDateTime lastPostAt; // [시니어 조치] 마지막 게시글 작성 시각

    private LocalDateTime revivedAt; // [시니어 조치] 깨우기 성공으로 인해 강제 활성화된 시각

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<NoticeTag> noticeTags = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.habidue.app.domain.usernotice.UserNotice> userNotices = new ArrayList<>();

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<com.habidue.app.domain.board.Post> posts = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
