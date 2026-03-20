package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 유저 활동 통계 엔티티 - 배지 부여 및 활동 지표 가시화의 기초 데이터
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_activity_stats")
public class UserActivityStats {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    private int totalPostCount = 0; // 총 게시글 수

    @Builder.Default
    private int totalCommentCount = 0; // 총 댓글 수

    @Builder.Default
    private int postLikeReceivedCount = 0; // [시니어 조치] 받은 게시글 좋아요 수

    @Builder.Default
    private int commentLikeReceivedCount = 0; // [시니어 조치] 받은 댓글 좋아요 수

    @Builder.Default
    private int totalViewReceivedCount = 0; // 받은 총 조회수

    @Builder.Default
    private int totalNoticeInterestCount = 0; // 관심 공고 설정 수

    @Builder.Default
    private int consecutiveAttendanceDays = 0; // 연속 출석 일수

    @Builder.Default
    private int totalAttendanceCount = 0; // [시니어 조치] 총 누적 출석 일수

    private LocalDate lastAttendanceDate; // 마지막 출석일

    @Builder.Default
    private int reviewPostCount = 0; // 후기 게시판 작성 수

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 통계 업데이트 편의 메서드들
     */
    public void incrementPostCount() { this.totalPostCount++; }
    public void decrementPostCount() { this.totalPostCount = Math.max(0, this.totalPostCount - 1); }
    
    public void incrementCommentCount() { this.totalCommentCount++; }
    public void decrementCommentCount() { this.totalCommentCount = Math.max(0, this.totalCommentCount - 1); }
    
    public void incrementPostLikeReceivedCount() { this.postLikeReceivedCount++; }
    public void decrementPostLikeReceivedCount() { this.postLikeReceivedCount = Math.max(0, this.postLikeReceivedCount - 1); }

    public void incrementCommentLikeReceivedCount() { this.commentLikeReceivedCount++; }
    public void decrementCommentLikeReceivedCount() { this.commentLikeReceivedCount = Math.max(0, this.commentLikeReceivedCount - 1); }
    
    public void incrementViewReceivedCount() { this.totalViewReceivedCount++; }
    
    public void incrementAttendanceCount() { this.totalAttendanceCount++; }
    
    public void incrementNoticeInterestCount() { this.totalNoticeInterestCount++; }
    public void decrementNoticeInterestCount() { this.totalNoticeInterestCount = Math.max(0, this.totalNoticeInterestCount - 1); }
    
    public void incrementReviewPostCount() { this.reviewPostCount++; }
    public void decrementReviewPostCount() { this.reviewPostCount = Math.max(0, this.reviewPostCount - 1); }

    public static UserActivityStats createEmpty(User user) {
        return UserActivityStats.builder()
                .user(user)
                .build();
    }
}
