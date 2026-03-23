package com.habidue.app.dto.admin;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationReportGroupDto {
    private String conversationId; // "user1Id-user2Id" 형태의 고유 키
    private String user1Name;
    private String user2Name;
    private ReportAdminResponseDto user1Report; // 유저1이 유저2를 신고한 내역
    private ReportAdminResponseDto user2Report; // 유저2가 유저1을 신고한 내역
}
