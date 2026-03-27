package com.habidue.app.dto.inquiry;

import com.habidue.app.domain.inquiry.Inquiry;
import com.habidue.app.domain.inquiry.InquiryCategory;
import com.habidue.app.domain.inquiry.InquiryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryResponseDto {
    private Long id;
    private String authorNickname;
    private InquiryCategory category;
    private String categoryDescription;
    private String title;
    private String content;
    private InquiryStatus status;
    private String statusDescription;
    private String answer;
    private String adminNickname;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private String imageUrl;

    public InquiryResponseDto(Inquiry inquiry) {
        this.id = inquiry.getId();
        this.authorNickname = inquiry.getAuthor().getNickname();
        this.category = inquiry.getCategory();
        this.categoryDescription = inquiry.getCategory().getDescription();
        this.title = inquiry.getTitle();
        this.content = inquiry.getContent();
        this.status = inquiry.getStatus();
        this.statusDescription = inquiry.getStatus().getDescription();
        this.answer = inquiry.getAnswer();
        this.adminNickname = inquiry.getAdmin() != null ? inquiry.getAdmin().getNickname() : null;
        this.answeredAt = inquiry.getAnsweredAt();
        this.createdAt = inquiry.getCreatedAt();
        this.imageUrl = inquiry.getImageUrl();
    }
}
