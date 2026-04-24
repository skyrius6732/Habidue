package com.habidue.app.wedding.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "wedding_account")
public class WeddingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", nullable = false)
    private WeddingInvitation invitation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountSide side;

    @Column(nullable = false, length = 30)
    private String bankName;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 30)
    private String accountHolder;

    @Column(length = 300)
    private String kakaoPayLink;
}
