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
    
    // [시니어 조치] 수신자 정보 추가 (대화방 목록에서 내가 보낸 경우 상대방 식별용)
    private Long receiverId;
    private String receiverNickname;

    private String content;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isSystem")
    private boolean isSystem;

    @JsonProperty("isDeleted")
    private boolean isDeleted; // [시니어 조치]

    @JsonProperty("isEdited")
    private boolean isEdited; // [시니어 조치]

    @JsonProperty("isReported")
    private boolean isReported;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Double aiScore;
    private Long unreadCount; // [시니어 조치] 대화방별 읽지 않은 메시지 수
    
    @JsonProperty("isPartnerOnline")
    private boolean isPartnerOnline; // [시니어 조치] 상대방 온라인 여부

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
        return from(message, null, false);
    }

    public static MessageResponseDto from(Message message, Long unreadCount, boolean isPartnerOnline) {
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
