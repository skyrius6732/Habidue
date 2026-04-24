package com.habidue.app.wedding.domain;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "wedding_rsvp")
public class WeddingRsvp extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private WeddingInvitation invitation;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean attendance;

    @Builder.Default
    private int guestCount = 1;

    private Boolean mealOption;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RsvpSide side = RsvpSide.UNKNOWN;

    @Column(length = 200)
    private String message;
}
