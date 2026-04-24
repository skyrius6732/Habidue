package com.habidue.app.wedding.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "wedding_photo")
public class WeddingPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private WeddingInvitation invitation;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int orderNum;
}
