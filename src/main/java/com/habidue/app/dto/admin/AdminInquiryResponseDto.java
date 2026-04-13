package com.habidue.app.dto.admin;

import com.habidue.app.domain.inquiry.Inquiry;
import com.habidue.app.domain.inquiry.InquiryCategory;
import com.habidue.app.domain.inquiry.InquiryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 관리자 전용 1:1 문의 응답 DTO (작성자 ID 포함)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminInquiryResponseDto {
    private Long id;
    private Long authorId; // 관리자용이므로 작성자 PK 포함
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

    public static AdminInquiryResponseDto from(Inquiry inquiry) {
        return AdminInquiryResponseDto.builder()
                .id(inquiry.getId())
                .authorId(inquiry.getAuthor().getId())
                .authorNickname(inquiry.getAuthor().getNickname())
                .category(inquiry.getCategory())
                .categoryDescription(inquiry.getCategory().getDescription())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .statusDescription(inquiry.getStatus().getDescription())
                .answer(inquiry.getAnswer())
                .adminNickname(inquiry.getAdmin() != null ? inquiry.getAdmin().getNickname() : null)
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .imageUrl(inquiry.getImageUrl())
                .build();
    }
}
