package com.habidue.app.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 유저 활동 통계 엔티티 - 배지 부여 및 활동 리포트용
 */
@Entity
@Getter
@Setter // [복구] AttendanceService 등에서 필드 수정을 위해 필요
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_activity_stats")
public class UserActivityStats {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    private int totalPostCount = 0;

    @Builder.Default
    private int totalCommentCount = 0;

    @Builder.Default
    private int postLikeReceivedCount = 0;

    @Builder.Default
    private int commentLikeReceivedCount = 0;

    @Builder.Default
    private int totalLikeReceivedCount = 0; // [시니어 조치] DB 정합성을 위해 복구 (게시글 + 댓글 합계)

    @Builder.Default
    private int totalViewReceivedCount = 0;

    @Builder.Default
    private int totalNoticeInterestCount = 0;

    @Builder.Default
    private int consecutiveAttendanceDays = 0;

    @Builder.Default
    private int maxConsecutiveAttendanceDays = 0; // [신규] 역대 최장 연속 출석 기록

    @Builder.Default
    private int totalAttendanceCount = 0;

    @Builder.Default
    private int reviewPostCount = 0;

    // [복구] 출석 체크 로직에 필수적인 필드
    private LocalDate lastAttendanceDate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // --- 비즈니스 로직 (증감 및 안전장치) ---

    public void incrementPostCount() { this.totalPostCount++; }
    public void decrementPostCount() { this.totalPostCount = Math.max(0, this.totalPostCount - 1); }

    public void incrementCommentCount() { this.totalCommentCount++; }
    public void decrementCommentCount() { this.totalCommentCount = Math.max(0, this.totalCommentCount - 1); }

    public void incrementPostLikeReceivedCount() { this.postLikeReceivedCount++; }
    public void decrementPostLikeReceivedCount() { this.postLikeReceivedCount = Math.max(0, this.postLikeReceivedCount - 1); }

    public void incrementCommentLikeReceivedCount() { this.commentLikeReceivedCount++; }
    public void decrementCommentLikeReceivedCount() { this.commentLikeReceivedCount = Math.max(0, this.commentLikeReceivedCount - 1); }

    // [수정] 인자 없는 버전과 있는 버전 모두 지원하여 PostService 오류 해결
    public void incrementViewReceivedCount() { this.totalViewReceivedCount++; }
    public void incrementViewReceivedCount(int count) { this.totalViewReceivedCount += count; }
    public void decrementViewReceivedCount(int count) { this.totalViewReceivedCount = Math.max(0, this.totalViewReceivedCount - count); }

    public void incrementNoticeInterestCount() { this.totalNoticeInterestCount++; }
    public void decrementNoticeInterestCount() { this.totalNoticeInterestCount = Math.max(0, this.totalNoticeInterestCount - 1); }

    // [복구] AttendanceService에서 사용
    public void incrementAttendanceCount() { this.totalAttendanceCount++; }

    public void updateAttendance(int consecutiveDays, int totalCount) {
        this.consecutiveAttendanceDays = consecutiveDays;
        this.totalAttendanceCount = totalCount;
    }

    public void incrementReviewPostCount() { this.reviewPostCount++; }
    public void decrementReviewPostCount() { this.reviewPostCount = Math.max(0, this.reviewPostCount - 1); }

    // [시니어 조치] 탈퇴 시 데이터 초기화 (StaleObjectStateException 방지를 위해 삭제 대신 초기화 사용)
    public void reset() {
        this.totalPostCount = 0;
        this.totalCommentCount = 0;
        this.postLikeReceivedCount = 0;
        this.commentLikeReceivedCount = 0;
        this.totalViewReceivedCount = 0;
        this.totalNoticeInterestCount = 0;
        this.consecutiveAttendanceDays = 0;
        this.totalAttendanceCount = 0;
        this.reviewPostCount = 0;
        this.lastAttendanceDate = null;
    }

    public static UserActivityStats createEmpty(User user) {
        return UserActivityStats.builder()
                .userId(user.getId())
                .user(user)
                .build();
    }
}
