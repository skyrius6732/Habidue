package com.habidue.app.dto.board;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.habidue.app.domain.board.Post;
import com.habidue.app.domain.board.PostType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName; // 닉네임
    private int authorLevel; // [시니어 조치] 작성자 레벨
    private long authorExp; // [시니어 조치] 작성자 경험치
    private String authorEquippedBadgeName; // [시니어 조치] 장착 중인 대표 배지 명칭
    private boolean showLevelEffects; // [시니어 조치] 작성자 효과 표시 설정
    private String authorEquippedEffect; // [시니어 조치] 작성자 특수 효과 코드
    private int authorKarmaPoint; // [시니어 조치] 작성자 카르마 점수 추가
    private Long noticeId;
    private String noticeTitle;
    private String noticeStatus; // [시니어 조치] 공고 마감 상태 전달
    
    @JsonProperty("isRevived")
    private boolean isRevived; // [시니어 조치] 공고 소통방 재활성화 여부

    @JsonProperty("isDormant")
    private boolean isDormant; // [시니어 조치] 휴면(보관중) 상태 여부
    
    private PostType type;
    private String category;
    private String subCategory; // [시니어 조치] 누락된 소메뉴 위치 필드 복구
    private String regionTag;
    private String status;
    private List<String> imageUrls;
    private List<com.habidue.app.dto.tag.TagResponseDto> tags; // 태그 리스트 추가
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private boolean isLiked;
    private List<com.habidue.app.dto.badge.BadgeResponseDto> authorBadges; // 작성자 획득 배지 리스트
    private Long prevId; // 이전글 ID
    private Long nextId; // 다음글 ID
    private boolean authorActive; // [시니어 조치] 작성자 탈퇴 여부 판단용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponseDto from(Post post) {
        boolean isActive = post.getAuthor().getStatus() == com.habidue.app.domain.user.UserStatus.ACTIVE;
        String displayName = isActive 
                ? (post.getAuthor().getNickname() != null ? post.getAuthor().getNickname() : post.getAuthor().getUsername())
                : "(탈퇴한 사용자)";

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorName(displayName)
                .authorActive(isActive)
                .authorLevel(post.getAuthor().getLevel())
                .authorExp(post.getAuthor().getTotalExp())
                .showLevelEffects(post.getAuthor().isShowLevelEffects())
                .authorEquippedEffect(post.getAuthor().getEquippedEffect())
                .authorKarmaPoint(post.getAuthor().getKarmaPoint()) // [시니어] 카르마 매핑
                .authorEquippedBadgeName(null) // 매퍼에서 후처리 필요 (Service 단에서 처리 권장)
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
                .subCategory(post.getSubCategory()) // [시니어 조치] 엔티티에서 값 추출
                .regionTag(post.getRegionTag())
                .status(post.getStatus())
                .imageUrls(post.getImages() != null ? post.getImages().stream().map(com.habidue.app.domain.board.PostImage::getImageUrl).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>())
                .tags(post.getTags() != null ? post.getTags().stream().map(pt -> new com.habidue.app.dto.tag.TagResponseDto(pt.getTag())).collect(java.util.stream.Collectors.toList()) : new java.util.ArrayList<>())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
