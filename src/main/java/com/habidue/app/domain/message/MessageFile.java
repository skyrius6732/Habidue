package com.habidue.app.domain.message;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "message_files")
public class MessageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false)
    private String fileUrl; // 저장된 경로 (URL)

    @Column(nullable = false)
    private String originalFileName; // 원본 파일명

    private String fileType; // 이미지, 문서 등 확장자

    private Long fileSize; // 파일 크기
}
