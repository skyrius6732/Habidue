package com.habidue.app.dto.barter;

import com.habidue.app.domain.barter.ProposalStatus;
import com.habidue.app.domain.board.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeProposalResponseDto {

    // [내부 DTO] 순환 참조 방지를 위한 간단한 Post 정보
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BarterPostInfoDto {
        private Long id;
        private String title;
        private String itemName;
        private String type;
        private String category;
        private String subCategory;
        private String authorName;
        private String authorPublicId;
        private List<String> imageUrls;
        // [바터 전용] 거래 선호 조건
        private com.habidue.app.domain.barter.TradeMethod preferredMethod;
        private java.time.LocalDate preferredDate;
        private String preferredTime;

        public static BarterPostInfoDto from(Post post) {
            return BarterPostInfoDto.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .itemName(post.getItemName())
                    .type(post.getType() != null ? post.getType().toString() : null)
                    .category(post.getCategory())
                    .subCategory(post.getSubCategory())
                    .authorName(post.getAuthor().getNickname())
                    .authorPublicId(post.getAuthor().getPublicId())
                    .imageUrls(post.getImages() != null ? post.getImages().stream()
                            .map(com.habidue.app.domain.board.PostImage::getImageUrl)
                            .collect(Collectors.toList()) : new ArrayList<>())
                    .preferredMethod(post.getPreferredMethod())
                    .preferredDate(post.getPreferredDate())
                    .preferredTime(post.getPreferredTime())
                    .build();
        }
    }

    private Long id;
    private String proposerPublicId;
    private String proposerNickname;
    private String receiverPublicId;
    private String receiverNickname;
    private BarterPostInfoDto barterPost;
    private BarterPostInfoDto offeredPost;
    private String message;
    private ProposalStatus status;
    private Integer questionCount;

    // [거래 완료 처리용 필드들]
    private LocalDateTime proposerCompletedAt;
    private LocalDateTime receiverCompletedAt;
    private LocalDateTime finalCompletedAt;

    // [협상 진행 정보]
    private Integer negotiationRound;
    private String lastScheduleSetBy;

    // [최종 합의된 거래 조건]
    private com.habidue.app.domain.barter.TradeMethod finalMethod;
    private String finalLocation;
    private String finalSenderAddress;
    private String finalReceiverAddress;
    private LocalDateTime finalTradeDateTime;

    // [차단 관련]
    private boolean blockedByPartner; // 상대방이 나를 차단했는지 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TradeProposalResponseDto from(com.habidue.app.domain.barter.TradeProposal proposal) {
        return TradeProposalResponseDto.builder()
                .id(proposal.getId())
                .proposerPublicId(proposal.getProposer().getPublicId())
                .proposerNickname(proposal.getProposer().getNickname())
                .receiverPublicId(proposal.getReceiver().getPublicId())
                .receiverNickname(proposal.getReceiver().getNickname())
                .barterPost(BarterPostInfoDto.from(proposal.getBarterPost()))
                .offeredPost(BarterPostInfoDto.from(proposal.getOfferedPost()))
                .message(proposal.getMessage())
                .status(proposal.getStatus())
                .questionCount(proposal.getQuestionCount())
                .proposerCompletedAt(proposal.getProposerCompletedAt())
                .receiverCompletedAt(proposal.getReceiverCompletedAt())
                .finalCompletedAt(proposal.getFinalCompletedAt())
                .negotiationRound(proposal.getNegotiationRound())
                .lastScheduleSetBy(proposal.getLastScheduleSetBy())
                .finalMethod(proposal.getFinalMethod())
                .finalLocation(proposal.getFinalLocation())
                .finalSenderAddress(proposal.getFinalSenderAddress())
                .finalReceiverAddress(proposal.getFinalReceiverAddress())
                .finalTradeDateTime(proposal.getFinalTradeDateTime())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}
