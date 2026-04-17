package com.habidue.app.domain.barter;


import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_proposal_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private TradeProposal proposal;

    @Column(nullable = false)
    private Integer round;                      // 협상 라운드

    @Column(length = 50, nullable = false)
    private String setBy;                       // PROPOSER / RECEIVER

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private TradeMethod method;                 // 이 라운드의 거래 방식

    @Column(length = 255)
    private String location;                    // 이 라운드의 장소

    @Column(length = 255)
    private String senderAddress;               // 이 라운드의 발송자 주소

    @Column(length = 255)
    private String receiverAddress;             // 이 라운드의 수신자 주소

    @Column(name = "trade_date_time")
    private LocalDateTime tradeDateTime;        // 이 라운드의 거래 시간

    @Column(length = 500)
    private String message;                     // 조건 제시 시 메시지

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
