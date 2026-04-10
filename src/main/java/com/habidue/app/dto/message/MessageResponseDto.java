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
    private String receiverPublicId; // [시니어 조치] 수신자 공개 ID 추가
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

    @JsonProperty("isPartnerWithdrawn")
    private boolean isPartnerWithdrawn; // [시니어 조치] 상대방이 탈퇴했는지 여부

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
        private String publicId; // [시니어 조치] 송신자 공개 ID 추가
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
     * [시니어 조치] 차단 및 탈퇴 정보를 포함하는 확장된 변환 메서드
     */
    public static MessageResponseDto from(Message message, Long unreadCount, boolean isPartnerOnline, boolean blockedByMe, boolean blockedByPartner) {
        SenderInfo senderInfo = null;
        boolean isSenderWithdrawn = false;
        if (message.getSender() != null) {
            isSenderWithdrawn = message.getSender().getStatus() == com.habidue.app.domain.user.UserStatus.WITHDRAWN;
            String nickname = isSenderWithdrawn ? "(탈퇴한 사용자)" : 
                             (message.getSender().getNickname() != null ? message.getSender().getNickname() : message.getSender().getUsername());
            senderInfo = new SenderInfo(message.getSender().getId(), message.getSender().getPublicId(), nickname);
        }

        String receiverNickname = null;
        boolean isReceiverWithdrawn = false;
        if (message.getReceiver() != null) {
            isReceiverWithdrawn = message.getReceiver().getStatus() == com.habidue.app.domain.user.UserStatus.WITHDRAWN;
            receiverNickname = isReceiverWithdrawn ? "(탈퇴한 사용자)" : 
                              (message.getReceiver().getNickname() != null ? message.getReceiver().getNickname() : message.getReceiver().getUsername());
        }

        // 시스템 메시지가 아닌 경우에만 상대방 탈퇴 여부 판단 (송신자 또는 수신자가 나일 때 반대편 기준)
        // 여기서는 단순하게 Receiver가 탈퇴했거나 Sender가 탈퇴했으면 true로 설정 (상황에 따라 정교화 가능)
        boolean partnerWithdrawn = isSenderWithdrawn || isReceiverWithdrawn;

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
                .receiverPublicId(message.getReceiver() != null ? message.getReceiver().getPublicId() : null)
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
                .isPartnerWithdrawn(partnerWithdrawn)
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
