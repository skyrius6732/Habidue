package com.habidue.app.dto.barter;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalRequestDto {
    private Long barterPostId;
    private Long offeredPostId;
    private String message;
}
