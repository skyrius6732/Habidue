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
     * [시니어 조치] 좋아요 알림과의 세트화를 위해 내부 알림 발송은 제거하고 결과만 반환
     */
    @Transactional
    public int restoreKarma(Long userId, int points, String comment, User admin, Integer maxAllowedTotal, KarmaReason reason) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getKarmaPoint() >= 1000) return 0;

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int dailyEarned = karmaHistoryRepository.getDailyEarnedPoints(userId, startOfDay);
        if (dailyEarned >= DAILY_MAX_RECOVERY) return 0;

        int change = Math.min(Math.abs(points), DAILY_MAX_RECOVERY - dailyEarned);
        int newPoint = Math.min(1000, user.getKarmaPoint() + change);
        int actualChange = newPoint - user.getKarmaPoint();
        if (actualChange <= 0) return 0;

        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason != null ? reason : KarmaReason.RESTORATION).pointChange(actualChange).resultingPoint(newPoint).comment(comment).admin(admin).build());
        
        if (newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) user.setStatus(UserStatus.ACTIVE);
        }
        return actualChange;
    }

    /**
     * [시니어 조치] 좋아요 취소 시 무조건 점수를 회수하되, 알림은 보내지 않음
     */
    @Transactional
    public int revokeKarma(Long userId, int points, String comment, KarmaReason reason) {
        User user = userRepository.findById(userId).orElseThrow();
        int scaledPoints = Math.abs(points);
        int newPoint = Math.max(0, user.getKarmaPoint() - scaledPoints);
        int actualChange = user.getKarmaPoint() - newPoint;
        if (actualChange <= 0) return 0;

        user.setKarmaPoint(newPoint);
        karmaHistoryRepository.save(KarmaHistory.builder().user(user).reason(reason).pointChange(-actualChange).resultingPoint(newPoint).comment(comment).build());
        return actualChange;
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
