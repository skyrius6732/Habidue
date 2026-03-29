package com.habidue.app.dto.board;

import com.habidue.app.domain.board.Comment;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private int authorLevel;
    private long authorExp;
    private int authorKarmaPoint;
    private String targetAuthorName;
    private Long parentId;
    private Long postId;
    private String status;
    private String createdAt;
    private List<com.habidue.app.dto.badge.BadgeResponseDto> authorBadges;
    private List<CommentResponseDto> children;
    private int likeCount;
    private boolean liked;
    private boolean authorActive; // [시니어 조치] 작성자 탈퇴 여부 판단용

    /**
     * [시니어 조치] 무한 재귀를 방지하기 위해 깊이(depth)를 1단계로 제한하는 안전한 변환 메서드
     */
    public static CommentResponseDto from(Comment comment, boolean isAdmin, Long currentUserId, com.habidue.app.repository.board.CommentLikeRepository likeRepository) {
        return toDto(comment, isAdmin, currentUserId, likeRepository, 0);
    }

    private static CommentResponseDto toDto(Comment comment, boolean isAdmin, Long currentUserId, com.habidue.app.repository.board.CommentLikeRepository likeRepository, int depth) {
        // [안전장치] 재귀 깊이가 3단계 이상이면 자식 생성을 중단 (무한 루프 방지)
        if (depth > 3) return null;

        String finalContent = comment.getContent();
        if ("USER_DELETED".equalsIgnoreCase(comment.getStatus())) {
            finalContent = isAdmin ? comment.getContent() : "작성자에 의해 삭제된 댓글입니다.";
        } else if ("DELETED".equalsIgnoreCase(comment.getStatus())) {
            finalContent = isAdmin ? comment.getContent() : "운영 정책 위반으로 영구 삭제된 댓글입니다.";
        }

        boolean isAuthorActive = comment.getAuthor().getStatus() == com.habidue.app.domain.user.UserStatus.ACTIVE;
        String displayName = isAuthorActive 
                ? (comment.getAuthor().getNickname() != null ? comment.getAuthor().getNickname() : comment.getAuthor().getUsername())
                : "(탈퇴한 사용자)";

        CommentResponseDto dto = CommentResponseDto.builder()
                .id(comment.getId())
                .content(finalContent)
                .authorId(comment.getAuthor().getId())
                .authorName(displayName)
                .authorActive(isAuthorActive)
                .authorLevel(comment.getAuthor().getLevel())
                .authorExp(comment.getAuthor().getTotalExp())
                .authorKarmaPoint(comment.getAuthor().getKarmaPoint())
                .targetAuthorName(comment.getParent() != null ? 
                        (comment.getParent().getAuthor().getNickname() != null ? comment.getParent().getAuthor().getNickname() : comment.getParent().getAuthor().getUsername()) 
                        : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .postId(comment.getPost().getId())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt().toString())
                .likeCount(comment.getLikeCount())
                .children(new ArrayList<>())
                .build();

        if (currentUserId != null && likeRepository != null) {
            dto.setLiked(likeRepository.existsByCommentAndUser(comment, com.habidue.app.domain.user.User.builder().id(currentUserId).build()));
        }

        // 자식 댓글 처리 (깊이 증가)
        if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
            dto.setChildren(comment.getChildren().stream()
                    .map(c -> toDto(c, isAdmin, currentUserId, likeRepository, depth + 1))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public static CommentResponseDto from(Comment comment, boolean isAdmin) {
        return toDto(comment, isAdmin, null, null, 0);
    }

    public static CommentResponseDto from(Comment comment) {
        return from(comment, false);
    }
}
