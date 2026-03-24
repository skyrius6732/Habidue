package com.habidue.app.dto.about;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRequestDto {
    private String tag;
    private String type;
    private String date;
    private String title;
    private String content;
    private boolean sendNotification; // [시니어 조치] 전체 알림 발송 여부
}
