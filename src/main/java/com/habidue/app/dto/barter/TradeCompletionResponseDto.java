package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.ProposalStatus;
import com.habidue.app.domain.barter.TradeProposal;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCompletionResponseDto {
    private Long id;
    private ProposalStatus status;
    private LocalDateTime completedAt;

    public static TradeCompletionResponseDto from(TradeProposal proposal) {
        return TradeCompletionResponseDto.builder()
                .id(proposal.getId())
                .status(proposal.getStatus())
                .completedAt(proposal.getFinalCompletedAt())
                .build();
    }
}
