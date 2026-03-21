package com.habidue.app.domain.message;

import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id") // 시스템 메시지일 경우 null 허용
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private boolean isRead = false;

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isSystem")
    private boolean isSystem = false; // 운영자/시스템 발송 여부

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false; // [시니어 조치] 시스템상 삭제 여부 (양쪽 모두에게 삭제 표시)

    @Builder.Default
    @Column(nullable = false)
    private boolean isEdited = false; // [시니어 조치] 수정 여부

    private LocalDateTime readAt;

    @Builder.Default
    private boolean deletedBySender = false;

    @Builder.Default
    private boolean deletedByReceiver = false;

    // AI 분석 결과
    private Double aiScore; // 0.0 ~ 1.0 (위험도)
    private String aiAnalysis; // AI의 간단한 판별 근거

    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isReported")
    private boolean isReported = false;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<MessageFile> attachments = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    public void addAttachment(MessageFile file) {
        this.attachments.add(file);
    }

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void deleteBySender() { this.deletedBySender = true; }
    public void deleteByReceiver() { this.deletedByReceiver = true; }
    public void report() { this.isReported = true; }
    
    // [시니어 조치] 추가 메서드
    public void softDelete() { this.isDeleted = true; }
    public void updateContent(String content) { 
        this.content = content; 
        this.isEdited = true; 
    }

    public void updateAiResult(Double score, String analysis) {
        this.aiScore = score;
        this.aiAnalysis = analysis;
    }
}
