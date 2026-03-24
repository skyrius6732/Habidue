package com.habidue.app.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.message.Message;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponseDto {
    private Long id;
    private SenderInfo sender;
    
    private Long receiverId;
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
    private boolean isBlockedByMe; // [시니어 조치] 내가 상대방을 차단했는지 여부

    @JsonProperty("isBlockedByPartner")
    private boolean isBlockedByPartner; // [시니어 조치] 상대방이 나를 차단했는지 여부

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
    private java.util.List<FileDto> attachments = new java.util.ArrayList<>();

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SenderInfo {
        private Long id;
        private String nickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileDto {
        private Long id;
        private String fileUrl;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
    }

    public static MessageResponseDto from(Message message) {
        return from(message, null, false, false, false);
    }

    public static MessageResponseDto from(Message message, Long unreadCount, boolean isPartnerOnline) {
        return from(message, unreadCount, isPartnerOnline, false, false);
    }

    /**
     * [시니어 조치] 차단 정보를 포함하는 확장된 변환 메서드
     */
    public static MessageResponseDto from(Message message, Long unreadCount, boolean isPartnerOnline, boolean blockedByMe, boolean blockedByPartner) {
        SenderInfo senderInfo = null;
        if (message.getSender() != null) {
            String nickname = message.getSender().getNickname() != null ? 
                             message.getSender().getNickname() : message.getSender().getUsername();
            senderInfo = new SenderInfo(message.getSender().getId(), nickname);
        }

        String receiverNickname = null;
        if (message.getReceiver() != null) {
            receiverNickname = message.getReceiver().getNickname() != null ? 
                              message.getReceiver().getNickname() : message.getReceiver().getUsername();
        }

        java.util.List<FileDto> attachments = message.getAttachments().stream()
                .map(f -> FileDto.builder()
                        .id(f.getId())
                        .fileUrl(f.getFileUrl())
                        .originalFileName(f.getOriginalFileName())
                        .fileType(f.getFileType())
                        .fileSize(f.getFileSize())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return MessageResponseDto.builder()
                .id(message.getId())
                .sender(senderInfo)
                .receiverId(message.getReceiver() != null ? message.getReceiver().getId() : null)
                .receiverNickname(receiverNickname)
                .content(message.getContent())
                .isRead(message.isRead())
                .isSystem(message.isSystem())
                .isDeleted(message.isDeleted())
                .isEdited(message.isEdited())
                .isRestoredByAdmin(message.isRestoredByAdmin())
                .isRoomRestricted(message.isRoomRestricted())
                .isBlockedByMe(blockedByMe)
                .isBlockedByPartner(blockedByPartner)
                .visibleToUserId(message.getVisibleToUserId())
                .relatedTargetId(message.getRelatedTargetId())
                .isReported(message.isReported())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .aiScore(message.getAiScore())
                .unreadCount(unreadCount)
                .isPartnerOnline(isPartnerOnline)
                .attachments(attachments)
                .build();
    }
}
