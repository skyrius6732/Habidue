package com.habidue.app.dto.usernotice;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNoticeUpdateDto {
    private String memo;
    private String referenceUrls;
    private LocalDateTime userDeadline; // 유저 개인 설정 마감일
}
