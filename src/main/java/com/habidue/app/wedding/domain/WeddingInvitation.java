package com.habidue.app.wedding.domain;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "wedding_invitation", uniqueConstraints = {
        @UniqueConstraint(columnNames = "slug")
})
public class WeddingInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.DRAFT;

    @Column(nullable = false, length = 50)
    private String groomName;

    @Column(nullable = false, length = 50)
    private String brideName;

    @Column(length = 50)
    private String groomFatherName;

    @Column(length = 50)
    private String groomMotherName;

    @Column(length = 50)
    private String brideFatherName;

    @Column(length = 50)
    private String brideMotherName;

    @Column(nullable = false)
    private LocalDateTime weddingDate;

    @Column(nullable = false, length = 100)
    private String venueName;

    @Column(nullable = false, length = 200)
    private String venueAddress;

    @Column(length = 100)
    private String venueDetailAddress;

    private Double venueLat;

    private Double venueLng;

    @Column(columnDefinition = "TEXT")
    private String greetingMessage;

    @Column(columnDefinition = "TEXT")
    private String transportInfo;

    @Column(length = 200)
    private String parkingInfo;

    @Column(length = 300)
    private String musicUrl;

    @Builder.Default
    private boolean musicAutoPlay = false;

    @Column(length = 50)
    @Builder.Default
    private String template = "default";

    @Builder.Default
    private int viewCount = 0;

    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNum ASC")
    @Builder.Default
    private List<WeddingPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeddingAccount> accounts = new ArrayList<>();

    public void update(String slug, String groomName, String brideName,
                       String groomFatherName, String groomMotherName,
                       String brideFatherName, String brideMotherName,
                       LocalDateTime weddingDate, String venueName, String venueAddress,
                       String venueDetailAddress, Double venueLat, Double venueLng,
                       String greetingMessage, String transportInfo, String parkingInfo,
                       String musicUrl, boolean musicAutoPlay, String template,
                       InvitationStatus status, LocalDateTime expiresAt) {
        this.slug = slug;
        this.groomName = groomName;
        this.brideName = brideName;
        this.groomFatherName = groomFatherName;
        this.groomMotherName = groomMotherName;
        this.brideFatherName = brideFatherName;
        this.brideMotherName = brideMotherName;
        this.weddingDate = weddingDate;
        this.venueName = venueName;
        this.venueAddress = venueAddress;
        this.venueDetailAddress = venueDetailAddress;
        this.venueLat = venueLat;
        this.venueLng = venueLng;
        this.greetingMessage = greetingMessage;
        this.transportInfo = transportInfo;
        this.parkingInfo = parkingInfo;
        this.musicUrl = musicUrl;
        this.musicAutoPlay = musicAutoPlay;
        this.template = template;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
