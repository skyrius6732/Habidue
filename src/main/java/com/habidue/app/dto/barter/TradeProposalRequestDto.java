package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.TradeMethod;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalRequestDto {
    private Long barterPostId;
    private Long offeredPostId;
    private String message;

    // [첫 제안시 자동 조건 설정]
    private TradeMethod proposerMethod;
    private String proposerLocation;
    private LocalDate proposerDate;
    private LocalTime proposerTime;
}
