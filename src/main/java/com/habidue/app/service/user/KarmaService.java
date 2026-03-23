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
public class KarmaService {

    private final UserRepository userRepository;
    private final KarmaHistoryRepository karmaHistoryRepository;
    private final NotificationService notificationService; // [시니어 조치] 알림 서비스 추가

    public static final int KARMA_SCALE = 10;
    public static final int DAILY_MAX_RECOVERY = 50;

    @Transactional
    public void deductKarma(Long userId, int points, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        int scaledPoints = Math.abs(points) * KARMA_SCALE;
        int originalPoint = user.getKarmaPoint();
        int newPoint = Math.max(0, originalPoint - scaledPoints);
        user.setKarmaPoint(newPoint);

        KarmaHistory history = KarmaHistory.builder()
                .user(user)
                .reason(reason)
                .pointChange(-scaledPoints)
                .resultingPoint(newPoint)
                .comment(comment)
                .admin(admin)
                .build();
        karmaHistoryRepository.save(history);

        // [시니어 조치] 신뢰 점수 감점 알림 발송
        String notiContent = String.format("⚖️ 신뢰 점수가 %.1fP 감점되었습니다. (사유: %s)", 
                (double)scaledPoints / KARMA_SCALE, reason.getDescription());
        notificationService.send(user, NotificationType.KARMA_CHANGE, notiContent, null);

        if (shouldTriggerBan) {
            applyAutomaticPenalty(user, newPoint);
        }
        
        log.info("Karma Deducted: User ID={}, Change={}, Final={}, TriggerBan={}", userId, -scaledPoints, newPoint, shouldTriggerBan);
    }

    @Transactional
    public void restoreKarma(Long userId, int points, String comment, User admin, Integer maxAllowedTotal, KarmaReason reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int dailyEarned = karmaHistoryRepository.getDailyEarnedPoints(userId, startOfDay);

        if (dailyEarned >= DAILY_MAX_RECOVERY) {
            return;
        }

        if (maxAllowedTotal != null && reason != null) {
            int earnedFromSource = karmaHistoryRepository.getPointsByPost(userId, reason, comment + "%");
            if (earnedFromSource >= maxAllowedTotal) {
                return;
            }
            points = Math.min(points, maxAllowedTotal - earnedFromSource);
        }

        int originalPoint = user.getKarmaPoint();
        int change = Math.abs(points);
        int allowedChange = Math.min(change, DAILY_MAX_RECOVERY - dailyEarned);
        if (allowedChange <= 0) return;

        int newPoint = originalPoint + allowedChange;
        user.setKarmaPoint(newPoint);

        KarmaHistory history = KarmaHistory.builder()
                .user(user)
                .reason(reason != null ? reason : KarmaReason.RESTORATION)
                .pointChange(allowedChange)
                .resultingPoint(newPoint)
                .comment(comment + (allowedChange < change ? " (상한 반영)" : ""))
                .admin(admin)
                .build();
        karmaHistoryRepository.save(history);

        // [시니어 조치] 신뢰 점수 회복 알림 발송
        String notiContent = String.format("⚖️ 신뢰 점수가 %.1fP 회복되었습니다. (사유: %s)", 
                (double)allowedChange / KARMA_SCALE, history.getReason().getDescription());
        notificationService.send(user, NotificationType.KARMA_CHANGE, notiContent, null);

        if (newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
        }
    }

    @Transactional
    public void manualAdjustKarma(Long userId, int delta, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        manualAdjustKarmaRaw(userId, delta * KARMA_SCALE, reason, comment, admin, shouldTriggerBan);
    }

    @Transactional
    public void manualAdjustKarmaRaw(Long userId, int rawDelta, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        int originalPoint = user.getKarmaPoint();
        int newPoint = Math.max(0, originalPoint + rawDelta);
        int actualChange = newPoint - originalPoint;
        
        user.setKarmaPoint(newPoint);

        KarmaHistory history = KarmaHistory.builder()
                .user(user)
                .reason(reason != null ? reason : KarmaReason.MANUAL_ADJUSTMENT)
                .pointChange(actualChange)
                .resultingPoint(newPoint)
                .comment(comment)
                .admin(admin)
                .build();
        karmaHistoryRepository.save(history);

        // [시니어 조치] 수동 조정 알림 발송
        if (actualChange != 0) {
            String direction = actualChange > 0 ? "상승" : "감점";
            String notiContent = String.format("⚖️ 신뢰 점수가 %.1fP %s되었습니다. (사유: %s)", 
                    Math.abs((double)actualChange / KARMA_SCALE), direction, history.getReason().getDescription());
            notificationService.send(user, NotificationType.KARMA_CHANGE, notiContent, null);
        }

        if (actualChange > 0 && newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
        } else if (actualChange < 0 && shouldTriggerBan) {
            applyAutomaticPenalty(user, newPoint);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<com.habidue.app.dto.user.KarmaHistoryResponseDto> getKarmaHistory(Long userId) {
        return karmaHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(com.habidue.app.dto.user.KarmaHistoryResponseDto::new)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void liftRestriction(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        user.setRestrictedUntil(null);
        if (user.getStatus() == UserStatus.RESTRICTED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        
        KarmaHistory history = KarmaHistory.builder()
                .user(user)
                .reason(KarmaReason.RESTORATION)
                .pointChange(0)
                .resultingPoint(user.getKarmaPoint())
                .comment("관리자에 의한 활동 제한 즉시 해제")
                .admin(admin)
                .build();
        karmaHistoryRepository.save(history);

        // [시니어 조치] 활동 제한 해제 알림 발송
        notificationService.send(user, NotificationType.SYSTEM, "📢 관리자에 의해 커뮤니티 활동 제한이 해제되었습니다. 다시 건전한 활동을 부탁드립니다.", null);
    }

    private void applyAutomaticPenalty(User user, int currentPoint) {
        LocalDateTime now = LocalDateTime.now();
        
        if (currentPoint <= 100) {
            user.setRestrictedUntil(now.plusYears(100));
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            notificationService.send(user, NotificationType.SYSTEM, "🚫 신뢰 점수 임계치 미달로 인해 커뮤니티 영구 활동 제한 조치가 취해졌습니다.", null);
        }
        else if (currentPoint <= 300) {
            user.setRestrictedUntil(now.plusDays(7));
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            notificationService.send(user, NotificationType.SYSTEM, "🚫 신뢰 점수 임계치 미달로 인해 7일간 커뮤니티 활동이 제한됩니다.", null);
        }
        else if (currentPoint <= 500) {
            user.setRestrictedUntil(now.plusDays(1));
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            notificationService.send(user, NotificationType.SYSTEM, "🚫 신뢰 점수 임계치 미달로 인해 24시간 동안 커뮤니티 활동이 제한됩니다.", null);
        }
        else {
            if (user.getRestrictedUntil() != null) {
                user.setRestrictedUntil(null);
                if (user.getStatus() == UserStatus.RESTRICTED) {
                    user.setStatus(UserStatus.ACTIVE);
                }
            }
        }
    }

    @Transactional
    public boolean isRestricted(User user) {
        if (user.getRestrictedUntil() == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        boolean restricted = user.getRestrictedUntil().isAfter(now);
        
        if (!restricted) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
        }
        
        return restricted;
    }
}
