package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.TradeMethod;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeScheduleRequestDto {
    private TradeMethod method;
    private String location;           // DIRECT: 공통 만남 장소
    private String senderAddress;      // DOORSTEP/PARCEL: 발송자 주소
    private String receiverAddress;    // DOORSTEP/PARCEL: 수신자 주소
    private LocalDateTime tradeDateTime;
    private String message;            // 조건 제시 시 메시지
}
