package com.habidue.app.service.user;

import com.habidue.app.domain.user.*;
import com.habidue.app.repository.user.KarmaHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KarmaService {

    private final UserRepository userRepository;
    private final KarmaHistoryRepository karmaHistoryRepository;
    private final NotificationService notificationService;

    public static final int KARMA_SCALE = 10;
    public static final int DAILY_MAX_RECOVERY = 50;

    @Transactional
    public void deductKarma(Long userId, int points, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        User user = userRepository.findById(userId).orElseThrow();
        int scaledPoints = Math.abs(points) * KARMA_SCALE;
        int newPoint = Math.max(0, user.getKarmaPoint() - scaledPoints);
        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason).pointChange(-scaledPoints).resultingPoint(newPoint).comment(comment).admin(admin).build());
        notificationService.send(user, NotificationType.KARMA_CHANGE, String.format("⚖️ 신뢰 점수가 %.1fP 감점되었습니다. (사유: %s)", (double)scaledPoints / KARMA_SCALE, reason.getDescription()), null, null);
        if (shouldTriggerBan) applyAutomaticPenalty(user, newPoint);
    }

    /**
     * [시니어 조치] 신뢰 점수 복구 (좋아요 수신 등)
     */
    @Transactional
    public int restoreKarma(Long userId, int points, String comment, User admin, Integer maxAllowedTotal, KarmaReason reason) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getKarmaPoint() >= 1000) return 0;

        // [시니어 조치] 게시글 단위 상한선(Cap) 정밀 체크
        // comment가 "POST_ID: 123" 형식이면 해당 게시글에서 발생한 모든 보상(좋아요 등)을 합산하여 체크함
        if (maxAllowedTotal != null && comment != null) {
            // "COMMENT_ID: ..." 같은 개별 댓글 식별자를 "POST_ID: ..."로 정규화하여 게시글 전체 한도 적용
            String groupKey = comment.startsWith("COMMENT_ID") ? extractPostKeyFromComment(comment) : comment;
            int alreadyEarned = karmaHistoryRepository.getSumByUserIdAndComment(userId, groupKey != null ? groupKey : comment);
            if (alreadyEarned >= maxAllowedTotal) {
                log.info("Karma Cap Reached for {}: {}", groupKey, alreadyEarned);
                return 0;
            }
            points = Math.min(points, maxAllowedTotal - alreadyEarned);
        }

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int dailyEarned = karmaHistoryRepository.getDailyEarnedPoints(userId, startOfDay);
        if (dailyEarned >= DAILY_MAX_RECOVERY) {
            log.info("Daily Karma Cap Reached for user {}", userId);
            return 0;
        }

        int change = Math.min(Math.abs(points), DAILY_MAX_RECOVERY - dailyEarned);
        int newPoint = Math.min(1000, user.getKarmaPoint() + change);
        int actualChange = newPoint - user.getKarmaPoint();
        if (actualChange <= 0) return 0;

        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason != null ? reason : KarmaReason.RESTORATION).pointChange(actualChange).resultingPoint(newPoint).comment(comment).admin(admin).build());
        
        if (newPoint >= 800 && user.getStatus() == UserStatus.RESTRICTED) {
            user.setRestrictedUntil(null);
            user.setStatus(UserStatus.ACTIVE);
        }
        return actualChange;
    }

    private String extractPostKeyFromComment(String comment) {
        if (comment == null) return null;
        // "COMMENT_ID: 123 (POST_ID: 456)" 또는 "POST_ID: 456" 에서 "POST_ID: 456"만 추출
        int startIndex = comment.indexOf("POST_ID:");
        if (startIndex != -1) {
            int endIndex = comment.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = comment.indexOf(")", startIndex);
            if (endIndex == -1) endIndex = comment.length();
            return comment.substring(startIndex, endIndex).trim();
        }
        return comment; 
    }

    /**
     * [시니어 조치] 좋아요 취소 시 점수 회수
     * 잔여 데이터가 상한선(max)을 여전히 충족한다면 점수를 회수하지 않는 '보존 로직' 적용
     */
    @Transactional
    public int revokeKarma(Long userId, int points, String comment, KarmaReason reason, Integer currentTotalActiveCount, Integer maxAllowedTotal) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // 1. 이전에 해당 항목으로 실제로 획득한 총 점수(Scaled) 확인
        int previouslyEarned = (comment != null) ? karmaHistoryRepository.getSumByUserIdAndComment(userId, comment) : 0;
        if (previouslyEarned <= 0) return 0;

        // 2. [시니어 조치] 잔여 기여도 기반 보존 로직
        // 예: 17개 좋아요 중 10개 삭제 -> 7개 남음. 1.0P(10) 중 7개분(0.7P)은 남겨야 함.
        if (currentTotalActiveCount != null) {
            int maxPoints = (maxAllowedTotal != null) ? maxAllowedTotal : 10; // 기본 상한 1.0P
            int shouldHavePoints = Math.min(currentTotalActiveCount, maxPoints); 
            int needToRevoke = Math.max(0, previouslyEarned - shouldHavePoints);
            
            if (needToRevoke <= 0) {
                log.info("잔여 활동({})이 충분하여 카르마를 보존합니다. (기존: {})", currentTotalActiveCount, previouslyEarned);
                return 0;
            }
            points = needToRevoke;
        }

        int pointsToRevoke = Math.min(Math.abs(points), previouslyEarned);
        int newPoint = Math.max(0, user.getKarmaPoint() - pointsToRevoke);
        int actualChange = user.getKarmaPoint() - newPoint;
        
        if (actualChange <= 0) return 0;

        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason).pointChange(-actualChange).resultingPoint(newPoint).comment(comment).build());
        return actualChange;
    }

    // 기존 하위 호환성을 위한 오버로딩
    @Transactional
    public int revokeKarma(Long userId, int points, String comment, KarmaReason reason) {
        return revokeKarma(userId, points, comment, reason, null, null);
    }

    @Transactional
    public void manualAdjustKarma(Long userId, int delta, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        manualAdjustKarmaRaw(userId, delta * KARMA_SCALE, reason, comment, admin, shouldTriggerBan);
    }

    @Transactional
    public void manualAdjustKarmaRaw(Long userId, int rawDelta, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        User user = userRepository.findById(userId).orElseThrow();
        int newPoint = Math.max(0, user.getKarmaPoint() + rawDelta);
        int actualChange = newPoint - user.getKarmaPoint();
        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason != null ? reason : KarmaReason.MANUAL_ADJUSTMENT).pointChange(actualChange).resultingPoint(newPoint).comment(comment).admin(admin).build());
        if (actualChange != 0) {
            String dir = actualChange > 0 ? "상승" : "감점";
            notificationService.send(user, NotificationType.KARMA_CHANGE, String.format("⚖️ 신뢰 점수가 %.1fP %s되었습니다. (사유: %s)", Math.abs((double)actualChange / KARMA_SCALE), dir, reason.getDescription()), null, null);
        }
        if (actualChange > 0 && newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) user.setStatus(UserStatus.ACTIVE);
        } else if (actualChange < 0 && shouldTriggerBan) {
            applyAutomaticPenalty(user, newPoint);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<com.habidue.app.dto.user.KarmaHistoryResponseDto> getKarmaHistory(Long userId) {
        return karmaHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(com.habidue.app.dto.user.KarmaHistoryResponseDto::new).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void liftRestriction(Long userId, User admin) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRestrictedUntil(null);
        if (user.getStatus() == UserStatus.RESTRICTED) user.setStatus(UserStatus.ACTIVE);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(KarmaReason.RESTORATION).pointChange(0).resultingPoint(user.getKarmaPoint()).comment("관리자에 의한 활동 제한 해제").admin(admin).build());
        notificationService.send(user, NotificationType.SYSTEM, "📢 관리자에 의해 커뮤니티 활동 제한이 해제되었습니다.", null, null);
    }

    private void applyAutomaticPenalty(User user, int currentPoint) {
        LocalDateTime now = LocalDateTime.now();
        if (currentPoint <= 100) { user.setRestrictedUntil(now.plusYears(100)); user.setStatus(UserStatus.RESTRICTED); notificationService.send(user, NotificationType.SYSTEM, "🚫 영구 활동 제한 조치", null, null); }
        else if (currentPoint <= 300) { user.setRestrictedUntil(now.plusDays(7)); user.setStatus(UserStatus.RESTRICTED); notificationService.send(user, NotificationType.SYSTEM, "🚫 7일 활동 제한", null, null); }
        else if (currentPoint <= 500) { user.setRestrictedUntil(now.plusDays(1)); user.setStatus(UserStatus.RESTRICTED); notificationService.send(user, NotificationType.SYSTEM, "🚫 24시간 활동 제한", null, null); }
    }

    @Transactional
    public boolean isRestricted(User user) {
        if (user.getRestrictedUntil() == null) return false;
        boolean restricted = user.getRestrictedUntil().isAfter(LocalDateTime.now());
        if (!restricted) { user.setRestrictedUntil(null); if (user.getStatus() == UserStatus.RESTRICTED) user.setStatus(UserStatus.ACTIVE); }
        return restricted;
    }
}
