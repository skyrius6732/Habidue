package com.habidue.app.dto.admin;

import com.habidue.app.domain.board.Comment;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자 전용 댓글 응답 DTO (ID 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCommentResponseDto {
    private Long id;
    private String content;
    private Long authorId; // 관리자용이므로 작성자 PK 포함
    private String authorPublicId;
    private String authorName;
    private int authorLevel;
    private long authorExp;
    private int authorKarmaPoint;
    private String authorEquippedEffect;
    private boolean showLevelEffects;
    private boolean showEquippedEffect;
    private Integer authorEquippedTier;
    private String targetAuthorName;
    private Long parentId;
    private Long postId;
    private String postTitle;
    private String status;
    private String createdAt;
    private List<com.habidue.app.dto.badge.BadgeResponseDto> authorBadges;
    private List<AdminCommentResponseDto> children;
    private int likeCount;
    private boolean liked;
    private boolean authorActive;

    public static AdminCommentResponseDto from(Comment comment, boolean isAdmin) {
        boolean isAuthorActive = comment.getAuthor().getStatus() == com.habidue.app.domain.user.UserStatus.ACTIVE;
        String displayName = isAuthorActive 
                ? (comment.getAuthor().getNickname() != null ? comment.getAuthor().getNickname() : comment.getAuthor().getUsername())
                : "(탈퇴한 사용자)";

        AdminCommentResponseDto dto = AdminCommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent()) // 관리자는 항상 실제 내용 확인 가능
                .authorId(comment.getAuthor().getId())
                .authorPublicId(comment.getAuthor().getPublicId())
                .authorName(displayName)
                .authorActive(isAuthorActive)
                .authorLevel(comment.getAuthor().getLevel())
                .authorExp(comment.getAuthor().getTotalExp())
                .authorKarmaPoint(comment.getAuthor().getKarmaPoint())
                .authorEquippedEffect(comment.getAuthor().getEquippedEffect())
                .showLevelEffects(comment.getAuthor().isShowLevelEffects())
                .showEquippedEffect(comment.getAuthor().isShowEquippedEffect())
                .authorEquippedTier(comment.getAuthor().getEquippedTier())
                .targetAuthorName(comment.getParent() != null ? 
                        (comment.getParent().getAuthor().getNickname() != null ? comment.getParent().getAuthor().getNickname() : comment.getParent().getAuthor().getUsername()) 
                        : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt().toString())
                .likeCount(comment.getLikeCount())
                .children(new ArrayList<>())
                .build();

        if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
            dto.setChildren(comment.getChildren().stream()
                    .map(c -> AdminCommentResponseDto.from(c, isAdmin))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
