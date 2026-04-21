package com.habidue.app.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 전용 게시글 응답 DTO (ID 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPostResponseDto {
    private Long id;
    private String title;
    private String content;
    private Long authorId; // 관리자용이므로 작성자 PK 포함
    private String authorPublicId;
    private String authorName;
    private int authorLevel;
    private long authorExp;
    private String authorEquippedBadgeName;
    private boolean showLevelEffects;
    private boolean showEquippedEffect;
    private Integer authorEquippedTier;
    private String authorEquippedEffect;
    private int authorKarmaPoint;
    private Long noticeId;
    private String noticeTitle;
    private String noticeStatus;
    
    @JsonProperty("isRevived")
    private boolean isRevived;

    @JsonProperty("isDormant")
    private boolean isDormant;
    
    private PostType type;
    private String category;
    private String subCategory;
    private String regionTag;
    private String status;
    private List<String> imageUrls;
    private List<com.habidue.app.dto.tag.TagResponseDto> tags;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private boolean isLiked;
    private List<com.habidue.app.dto.badge.BadgeResponseDto> authorBadges;
    private Long prevId;
    private Long nextId;
    private boolean authorActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 물물교환 필드
    private String itemName;
    private String wantedItem;
    private com.habidue.app.domain.barter.ItemCondition itemCondition;
    private com.habidue.app.domain.barter.BarterStatus barterStatus;
    private com.habidue.app.domain.barter.TradeMethod preferredMethod;
    private java.time.LocalDate preferredDate;
    private String preferredTime;

    public static AdminPostResponseDto from(Post post) {
        boolean isActive = post.getAuthor().getStatus() == com.habidue.app.domain.user.UserStatus.ACTIVE;
        String displayName = isActive 
                ? (post.getAuthor().getNickname() != null ? post.getAuthor().getNickname() : post.getAuthor().getUsername())
                : "(탈퇴한 사용자)";

        return AdminPostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorPublicId(post.getAuthor().getPublicId())
                .authorName(displayName)
                .authorActive(isActive)
                .authorLevel(post.getAuthor().getLevel())
                .authorExp(post.getAuthor().getTotalExp())
                .showLevelEffects(post.getAuthor().isShowLevelEffects())
                .showEquippedEffect(post.getAuthor().isShowEquippedEffect())
                .authorEquippedTier(post.getAuthor().getEquippedTier())
                .authorEquippedEffect(post.getAuthor().getEquippedEffect())
                .authorKarmaPoint(post.getAuthor().getKarmaPoint())
                .noticeId(post.getNotice() != null ? post.getNotice().getId() : null)
                .noticeTitle(post.getNotice() != null ? post.getNotice().getTitle() : null)
                .noticeStatus(post.getNotice() != null ? post.getNotice().getStatus().name() : null)
                .isRevived(post.getNotice() != null && post.getNotice().getRevivedAt() != null && 
                           post.getNotice().getRevivedAt().isAfter(java.time.LocalDateTime.now().minusDays(7)))
                .isDormant(post.getNotice() != null && Boolean.TRUE.equals(post.getNotice().getIsBoardActive()) && 
                           (post.getNotice().getRevivedAt() == null || post.getNotice().getRevivedAt().isBefore(java.time.LocalDateTime.now().minusDays(7))) &&
                           (post.getNotice().getLastPostAt() != null ? post.getNotice().getLastPostAt() : post.getNotice().getUpdatedAt())
                           .isBefore(java.time.LocalDateTime.now().minusDays(7)))
                .type(post.getType())
                .category(post.getCategory())
                .subCategory(post.getSubCategory())
                .regionTag(post.getRegionTag())
                .status(post.getStatus())
                .imageUrls(post.getImages() != null ? post.getImages().stream().map(com.habidue.app.domain.board.PostImage::getImageUrl).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>())
                .tags(post.getTags() != null ? post.getTags().stream().map(pt -> new com.habidue.app.dto.tag.TagResponseDto(pt.getTag())).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .itemName(post.getItemName())
                .wantedItem(post.getWantedItem())
                .itemCondition(post.getItemCondition())
                .barterStatus(post.getBarterStatus())
                .preferredMethod(post.getPreferredMethod())
                .preferredDate(post.getPreferredDate())
                .preferredTime(post.getPreferredTime())
                .build();
    }
}
