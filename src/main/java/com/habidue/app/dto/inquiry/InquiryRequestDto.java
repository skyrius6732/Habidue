package com.habidue.app.dto.inquiry;

import com.habidue.app.domain.inquiry.InquiryCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryRequestDto {
    private String title;
    private String content;
    private InquiryCategory category;
}
