package com.habidue.app.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.message.Message;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자 전용 쪽지 응답 DTO (ID 포함)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminMessageResponseDto {
    private Long id;
    private AdminSenderInfo sender;
    
    private Long receiverId;
    private String receiverPublicId;
    private String receiverNickname;

    private String content;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isSystem")
    private boolean isSystem;

    @JsonProperty("isDeleted")
    private boolean isDeleted;

    @JsonProperty("isEdited")
    private boolean isEdited;

    @JsonProperty("isRestoredByAdmin")
    private boolean isRestoredByAdmin;

    @JsonProperty("isRoomRestricted")
    private boolean isRoomRestricted;

    @JsonProperty("isBlockedByMe")
    private boolean isBlockedByMe;

    @JsonProperty("isBlockedByPartner")
    private boolean isBlockedByPartner;

    @JsonProperty("isPartnerWithdrawn")
    private boolean isPartnerWithdrawn;

    private Long visibleToUserId;
    private Long relatedTargetId;

    @JsonProperty("isReported")
    private boolean isReported;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Double aiScore;
    private Long unreadCount;
    
    @JsonProperty("isPartnerOnline")
    private boolean isPartnerOnline;

    @Builder.Default
    private List<AdminFileDto> attachments = new java.util.ArrayList<>();

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminSenderInfo {
        private Long id;
        private String publicId;
        private String nickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdminFileDto {
        private Long id;
        private String fileUrl;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
    }

    public static AdminMessageResponseDto from(Message message) {
        AdminSenderInfo senderInfo = null;
        if (message.getSender() != null) {
            senderInfo = new AdminSenderInfo(
                message.getSender().getId(), 
                message.getSender().getPublicId(), 
                message.getSender().getNickname() != null ? message.getSender().getNickname() : message.getSender().getUsername()
            );
        }

        List<AdminFileDto> attachments = message.getAttachments().stream()
                .map(f -> AdminFileDto.builder()
                        .id(f.getId())
                        .fileUrl(f.getFileUrl())
                        .originalFileName(f.getOriginalFileName())
                        .fileType(f.getFileType())
                        .fileSize(f.getFileSize())
                        .build())
                .collect(Collectors.toList());

        return AdminMessageResponseDto.builder()
                .id(message.getId())
                .sender(senderInfo)
                .receiverId(message.getReceiver() != null ? message.getReceiver().getId() : null)
                .receiverPublicId(message.getReceiver() != null ? message.getReceiver().getPublicId() : null)
                .receiverNickname(message.getReceiver() != null ? (message.getReceiver().getNickname() != null ? message.getReceiver().getNickname() : message.getReceiver().getUsername()) : null)
                .content(message.getContent())
                .isRead(message.isRead())
                .isSystem(message.isSystem())
                .isDeleted(message.isDeleted())
                .isEdited(message.isEdited())
                .isRestoredByAdmin(message.isRestoredByAdmin())
                .isRoomRestricted(message.isRoomRestricted())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .aiScore(message.getAiScore())
                .attachments(attachments)
                .build();
    }
}
