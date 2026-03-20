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
    private String content;

    @JsonProperty("isRead")
    private boolean isRead;

    @JsonProperty("isSystem")
    private boolean isSystem;

    @JsonProperty("isReported")
    private boolean isReported;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Double aiScore;
    private java.util.List<FileDto> files;

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
        SenderInfo senderInfo = null;
        if (message.getSender() != null) {
            String nickname = message.getSender().getNickname() != null ? 
                             message.getSender().getNickname() : message.getSender().getUsername();
            senderInfo = new SenderInfo(message.getSender().getId(), nickname);
        }

        java.util.List<FileDto> files = message.getAttachments().stream()
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
                .content(message.getContent())
                .isRead(message.isRead())
                .isSystem(message.isSystem())
                .isReported(message.isReported())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .aiScore(message.getAiScore())
                .files(files)
                .build();
    }
}
