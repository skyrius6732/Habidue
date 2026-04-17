package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.TradeProposalHistory;
import com.habidue.app.domain.barter.TradeMethod;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalHistoryResponseDto {
    private Long id;
    private Integer round;
    private String setBy;
    private TradeMethod method;
    private String location;
    private String senderAddress;
    private String receiverAddress;
    private LocalDateTime tradeDateTime;
    private String message;
    private LocalDateTime createdAt;

    public static TradeProposalHistoryResponseDto from(TradeProposalHistory history) {
        return TradeProposalHistoryResponseDto.builder()
                .id(history.getId())
                .round(history.getRound())
                .setBy(history.getSetBy())
                .method(history.getMethod())
                .location(history.getLocation())
                .senderAddress(history.getSenderAddress())
                .receiverAddress(history.getReceiverAddress())
                .tradeDateTime(history.getTradeDateTime())
                .message(history.getMessage())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
