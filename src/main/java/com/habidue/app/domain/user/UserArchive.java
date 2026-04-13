package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * [시니어 조치] 탈퇴 유저 증거 보존용 아카이브 엔티티 (고도화 버전)
 * - 향후 확장성(전화번호, 주소, 실명 등)을 고려하여 미리 필드를 확보
 * - rawAttributes 필드를 통해 OAuth2 제공자의 원본 데이터를 유연하게 보관 가능
 */
@Entity
@Table(name = "user_archives", indexes = {
    @Index(name = "idx_archive_target_user_id", columnList = "targetUserId"),
    @Index(name = "idx_archive_email", columnList = "email"),
    @Index(name = "idx_archive_phone", columnList = "phoneNumber")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long targetUserId; // 원본 users 테이블의 PK

    private String username;
    private String email;
    private String nickname;
    private String provider;
    private String providerId;

    // [향후 확장 필드] 당장은 null로 저장되지만, 수사 및 데이터 마이그레이션 대비
    private String realName;    // 실명
    private String phoneNumber; // 연락처
    private String address;     // 주소

    @Column(columnDefinition = "TEXT")
    private String rawAttributes; // OAuth2 제공자의 전체 응답 데이터 (JSON 형태 저장용)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime withdrawnAt; // 탈퇴(아카이브) 시점

    public static UserArchive from(User user) {
        return UserArchive.builder()
                .targetUserId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                // realName, phoneNumber 등은 현재 User 엔티티에 없으므로 null로 유지
                // 나중에 User 엔티티에 추가되면 여기서 매핑만 추가하면 됨
                .build();
    }
}
