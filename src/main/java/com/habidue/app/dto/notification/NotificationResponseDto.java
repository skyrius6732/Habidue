package com.habidue.app.dto.notification;

import com.habidue.app.domain.notification.Notification;
import com.habidue.app.domain.notification.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private Long id;
    private NotificationType type;
    private String icon;
    private String content;
    private Long relatedTargetId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification n) {
        return NotificationResponseDto.builder()
                .id(n.getId())
                .type(n.getType())
                .icon(n.getType().getIcon())
                .content(n.getContent())
                .relatedTargetId(n.getRelatedTargetId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
