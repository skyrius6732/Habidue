package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.ProposalStatus;
import com.habidue.app.dto.board.PostResponseDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalResponseDto {
    private Long id;
    private String proposerPublicId;
    private String proposerNickname;
    private String receiverPublicId;
    private String receiverNickname;
    private PostResponseDto barterPost;
    private PostResponseDto offeredPost;
    private String message;
    private ProposalStatus status;
    private Integer questionCount;

    // [거래 조건 협상용 필드들]
    private String proposerScheduleJson;
    private String receiverScheduleJson;
    private String agreedScheduleJson;

    // [거래 완료 처리용 필드들]
    private LocalDateTime proposerCompletedAt;
    private LocalDateTime receiverCompletedAt;
    private LocalDateTime finalCompletedAt;

    // [협상 진행 정보]
    private Integer negotiationRound;
    private String lastScheduleSetBy;

    // [차단 관련]
    private boolean blockedByPartner; // 상대방이 나를 차단했는지 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TradeProposalResponseDto from(com.habidue.app.domain.barter.TradeProposal proposal) {
        // PostResponseDto.from() 대신 필요한 필드만 수동 매핑 (Lazy 필드 접근 방지)
        PostResponseDto barterPostDto = PostResponseDto.builder()
                .id(proposal.getBarterPost().getId())
                .title(proposal.getBarterPost().getTitle())
                .type(proposal.getBarterPost().getType())
                .category(proposal.getBarterPost().getCategory())
                .subCategory(proposal.getBarterPost().getSubCategory())
                .authorName(proposal.getBarterPost().getAuthor().getNickname())
                .authorPublicId(proposal.getBarterPost().getAuthor().getPublicId())
                .imageUrls(new java.util.ArrayList<>()) // 빈 리스트 (Lazy 필드 접근 방지)
                .build();

        PostResponseDto offeredPostDto = PostResponseDto.builder()
                .id(proposal.getOfferedPost().getId())
                .title(proposal.getOfferedPost().getTitle())
                .type(proposal.getOfferedPost().getType())
                .category(proposal.getOfferedPost().getCategory())
                .subCategory(proposal.getOfferedPost().getSubCategory())
                .authorName(proposal.getOfferedPost().getAuthor().getNickname())
                .authorPublicId(proposal.getOfferedPost().getAuthor().getPublicId())
                .imageUrls(new java.util.ArrayList<>()) // 빈 리스트 (Lazy 필드 접근 방지)
                .build();

        return TradeProposalResponseDto.builder()
                .id(proposal.getId())
                .proposerPublicId(proposal.getProposer().getPublicId())
                .proposerNickname(proposal.getProposer().getNickname())
                .receiverPublicId(proposal.getReceiver().getPublicId())
                .receiverNickname(proposal.getReceiver().getNickname())
                .barterPost(barterPostDto)
                .offeredPost(offeredPostDto)
                .message(proposal.getMessage())
                .status(proposal.getStatus())
                .questionCount(proposal.getQuestionCount())
                // [새로운 필드들 추가]
                .proposerScheduleJson(proposal.getProposerScheduleJson())
                .receiverScheduleJson(proposal.getReceiverScheduleJson())
                .agreedScheduleJson(proposal.getAgreedScheduleJson())
                .proposerCompletedAt(proposal.getProposerCompletedAt())
                .receiverCompletedAt(proposal.getReceiverCompletedAt())
                .finalCompletedAt(proposal.getFinalCompletedAt())
                .negotiationRound(proposal.getNegotiationRound())
                .lastScheduleSetBy(proposal.getLastScheduleSetBy())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}
