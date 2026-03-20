package com.habidue.app.dto.board;

import com.habidue.app.domain.board.Comment;
import lombok.*;


import java.util.List;


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
    private int authorLevel; // [시니어 조치] 작성자 레벨
    private long authorExp; // [시니어 조치] 작성자 경험치
    private int authorKarmaPoint; // [시니어 조치] 작성자 카르마 점수 추가
    private String targetAuthorName; // [추가] 답글 대상자 이름
    private Long parentId;
    private Long postId; // [시니어 조치] 해당 댓글이 속한 게시글 ID
    private String status; // [추가] 상태 (ACTIVE, BLINDED 등)
    private String createdAt; // [수정] 포맷팅된 날짜를 위해 String 권장 (또는 기존 유지)
    private List<com.habidue.app.dto.badge.BadgeResponseDto> authorBadges; // [시니어 조치] 작성자 배지 리스트
    private List<CommentResponseDto> children;
    private int likeCount; // [시니어] 좋아요 카운트 추가
    private boolean liked; // [시니어] 로그인한 유저의 좋아요 여부

    public static CommentResponseDto from(com.habidue.app.domain.board.Comment comment) {
        return from(comment, false); // 기본값은 마스킹 적용
    }

    public static CommentResponseDto from(com.habidue.app.domain.board.Comment comment, boolean isAdmin) {
        // [시니어 조치] 상태에 따른 내용 마스킹 정책 (관리자는 예외)
        String finalContent = comment.getContent();
        if (!isAdmin && "DELETED".equalsIgnoreCase(comment.getStatus())) {
            finalContent = "운영 정책 위반으로 영구 삭제된 댓글입니다.";
        }

        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(finalContent)
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getNickname() != null ? comment.getAuthor().getNickname() : comment.getAuthor().getUsername())
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
                .children(comment.getChildren().stream()
                        .map(c -> CommentResponseDto.from(c, isAdmin)) // 재귀 호출 시 권한 전파
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    public static CommentResponseDto from(com.habidue.app.domain.board.Comment comment, boolean isAdmin, Long currentUserId, com.habidue.app.repository.board.CommentLikeRepository likeRepository) {
        CommentResponseDto dto = from(comment, isAdmin);
        if (currentUserId != null && likeRepository != null) {
            dto.setLiked(likeRepository.existsByCommentAndUser(comment, com.habidue.app.domain.user.User.builder().id(currentUserId).build()));
        }
        
        // 자식 댓글들도 좋아요 여부 재귀 처리
        if (dto.getChildren() != null && comment.getChildren() != null) {
            for (int i = 0; i < comment.getChildren().size(); i++) {
                com.habidue.app.domain.board.Comment childEntity = comment.getChildren().get(i);
                CommentResponseDto childDto = dto.getChildren().get(i);
                if (currentUserId != null && likeRepository != null) {
                    childDto.setLiked(likeRepository.existsByCommentAndUser(childEntity, com.habidue.app.domain.user.User.builder().id(currentUserId).build()));
                }
                childDto.setLikeCount(childEntity.getLikeCount());
            }
        }
        return dto;
    }
}
