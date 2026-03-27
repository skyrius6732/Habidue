package com.habidue.app.domain.inquiry;

import com.habidue.app.domain.common.BaseTimeEntity;
import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.RECEIVED;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin; // 답변한 관리자

    private LocalDateTime answeredAt;

    private String imageUrl; // 첨부 이미지 URL

    @Builder.Default
    private boolean deletedByAuthor = false; // 사용자에 의한 삭제 여부 (관리자는 계속 볼 수 있음)

    public void update(String title, String content, InquiryCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void deleteByAuthor() {
        this.deletedByAuthor = true;
    }

    public void answer(String answer, User admin) {
        this.answer = answer;
        this.admin = admin;
        this.answeredAt = LocalDateTime.now();
        this.status = InquiryStatus.ANSWERED;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }
}
