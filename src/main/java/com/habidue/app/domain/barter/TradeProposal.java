package com.habidue.app.domain.barter;

import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_proposals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barter_post_id", nullable = false)
    private Post barterPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_post_id", nullable = false)
    private Post offeredPost;

    @Column(length = 100)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PROPOSED;

    @Column(nullable = false)
    @Builder.Default
    private Integer questionCount = 0;

    // [거래 조건 협상용 필드들]
    @Column(columnDefinition = "LONGTEXT")
    private String proposerScheduleJson;      // 제안자가 제시한 거래 조건

    @Column(columnDefinition = "LONGTEXT")
    private String receiverScheduleJson;      // 수신자가 제시한 거래 조건

    @Column(columnDefinition = "LONGTEXT")
    private String agreedScheduleJson;        // 양쪽이 합의한 최종 조건

    // [거래 완료 처리용 필드들]
    @Column(name = "proposer_completed_at", nullable = true)
    private LocalDateTime proposerCompletedAt;  // 제안자가 "거래완료" 누른 시간

    @Column(name = "receiver_completed_at", nullable = true)
    private LocalDateTime receiverCompletedAt;  // 수신자가 "거래완료" 누른 시간

    @Column(name = "final_completed_at", nullable = true)
    private LocalDateTime finalCompletedAt;     // 양쪽이 모두 완료한 시간

    // [협상 진행 정보]
    @Builder.Default
    private Integer negotiationRound = 0;       // 협상 라운드 (0부터 시작)

    @Column(length = 20)
    private String lastScheduleSetBy;           // 마지막 조건 설정자 (PROPOSER/RECEIVER)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void accept() {
        this.status = ProposalStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ProposalStatus.REJECTED;
    }

    public void cancel() {
        this.status = ProposalStatus.CANCELLED;
    }

    public void complete() {
        this.status = ProposalStatus.COMPLETED;
    }

    public void incrementQuestionCount() {
        this.questionCount++;
    }
}
