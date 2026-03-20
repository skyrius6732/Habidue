package com.habidue.app.service.user;

import com.habidue.app.domain.user.*;
import com.habidue.app.repository.user.KarmaHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
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

    // [시니어] 카르마 운영 정책 상수화 (1점 = 10포인트 스케일링)
    public static final int KARMA_SCALE = 10;
    public static final int DAILY_MAX_RECOVERY = 50; // 일일 최대 5.0점까지만 회복 가능 (면죄부 방지)

    /**
     * [시니어 페널티 로직] 활동 신뢰 점수(Karma) 차감 (제재 트리거 여부 선택 가능)
     */
    @Transactional
    public void deductKarma(Long userId, int points, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        int scaledPoints = Math.abs(points) * KARMA_SCALE; // 수치 10배 적용
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

        // [시니어] 명시적으로 제재 발동을 요청한 경우에만 체크 (임계치도 10배 적용)
        if (shouldTriggerBan) {
            applyAutomaticPenalty(user, newPoint);
        }
        
        log.info("Karma Deducted: User ID={}, Change={}, Final={}, TriggerBan={}", userId, -scaledPoints, newPoint, shouldTriggerBan);
    }

    /**
     * [시니어 복구 로직] 활동 신뢰 점수(Karma) 복구 (자동 로직용, 항상 가산)
     * [운영 정책] 일일 복구 상한선(DAILY_MAX_RECOVERY) 적용 및 특정 소스(게시물 등)별 상한 설정 가능
     */
    @Transactional
    public void restoreKarma(Long userId, int points, String comment, User admin, Integer maxAllowedTotal, KarmaReason reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 1. 일일 총합 제한 체크 (공통 방어선)
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        int dailyEarned = karmaHistoryRepository.getDailyEarnedPoints(userId, startOfDay);

        if (dailyEarned >= DAILY_MAX_RECOVERY) {
            log.info("Karma Recovery Skip: Daily limit reached for User ID={}", userId);
            return;
        }

        // 2. 특정 소스(게시물 등)별 상한 체크 (어뷰징 세탁 방지)
        if (maxAllowedTotal != null && reason != null) {
            int earnedFromSource = karmaHistoryRepository.getPointsByPost(userId, reason, comment + "%");
            if (earnedFromSource >= maxAllowedTotal) {
                log.info("Karma Recovery Skip: Per-source limit reached for User ID={}, Source={}", userId, comment);
                return;
            }
            points = Math.min(points, maxAllowedTotal - earnedFromSource);
        }

        int originalPoint = user.getKarmaPoint();
        int change = Math.abs(points);
        
        // 잔여 일일 허용치 계산
        int allowedChange = Math.min(change, DAILY_MAX_RECOVERY - dailyEarned);
        if (allowedChange <= 0) return;

        int newPoint = originalPoint + allowedChange;
        user.setKarmaPoint(newPoint);

        // 점수 변화 기록
        KarmaHistory history = KarmaHistory.builder()
                .user(user)
                .reason(reason != null ? reason : KarmaReason.RESTORATION)
                .pointChange(allowedChange)
                .resultingPoint(newPoint)
                .comment(comment + (allowedChange < change ? " (상한 반영)" : ""))
                .admin(admin)
                .build();
        karmaHistoryRepository.save(history);

        // [시니어] 점수 회복 시 상태 정상화 (임계치 800)
        if (newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
        }
    }

    /**
     * [시니어 수동 조정] 관리자/시스템에 의한 점수 조정 (제재 트리거 여부 선택 가능)
     */
    @Transactional
    public void manualAdjustKarma(Long userId, int delta, KarmaReason reason, String comment, User admin, boolean shouldTriggerBan) {
        manualAdjustKarmaRaw(userId, delta * KARMA_SCALE, reason, comment, admin, shouldTriggerBan);
    }

    /**
     * [시니어 정밀 조정] 내부 포인트를 직접 전달받아 조정 (0.1P 단위 대응)
     */
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

        // [시니어] 점수 상승 시 제재 해제, 점수 하락 시 요청에 따라 제재 트리거 (800포인트 기준)
        if (actualChange > 0 && newPoint >= 800) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
        } else if (actualChange < 0 && shouldTriggerBan) {
            applyAutomaticPenalty(user, newPoint);
        }
    }

    /**
     * 특정 유저의 모든 카르마 히스토리 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<com.habidue.app.dto.user.KarmaHistoryResponseDto> getKarmaHistory(Long userId) {
        return karmaHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(com.habidue.app.dto.user.KarmaHistoryResponseDto::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * [시니어 제재 해제] 점수와 상관없이 즉시 활동 제한만 해제
     */
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
    }

    /**
     * [자동 임계치 체크] 사용자 요청 규칙에 따른 단계별 제재 적용
     */
    private void applyAutomaticPenalty(User user, int currentPoint) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 영구 제재 (100포인트 이하 = 기존 10점)
        if (currentPoint <= 100) {
            user.setRestrictedUntil(now.plusYears(100)); // 사실상 영구
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            log.warn("PERMANENT PENALTY Applied: User ID={}, Karma={}", user.getId(), currentPoint);
        }
        // 2. 격리 구간 (300포인트 이하 = 기존 30점) - 7일 제한
        else if (currentPoint <= 300) {
            user.setRestrictedUntil(now.plusDays(7)); // 현재로부터 7일 후
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            log.info("7-DAY PENALTY Applied: User ID={}, Karma={}", user.getId(), currentPoint);
        }
        // 3. 제한 구간 (500포인트 이하 = 기존 50점) - 24시간 제한
        else if (currentPoint <= 500) {
            user.setRestrictedUntil(now.plusDays(1)); // 현재로부터 24시간 후
            user.setStatus(UserStatus.RESTRICTED);
            user.setPenaltyCount(user.getPenaltyCount() + 1);
            log.info("24-HOUR PENALTY Applied: User ID={}, Karma={}", user.getId(), currentPoint);
        }
        // 4. 경고 구간 (800포인트 이하 = 기존 80점) 및 정상 유저
        else {
            // 점수가 500포인트를 초과하면 기존 제한을 해제 (수동 조정 등 고려)
            if (user.getRestrictedUntil() != null) {
                user.setRestrictedUntil(null);
                if (user.getStatus() == UserStatus.RESTRICTED) {
                    user.setStatus(UserStatus.ACTIVE);
                }
            }
        }
    }

    /**
     * 현재 유저가 활동 제한 상태인지 확인 (시간이 지나면 자동으로 데이터 청소 및 상태 복구)
     */
    @Transactional
    public boolean isRestricted(User user) {
        if (user.getRestrictedUntil() == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        boolean restricted = user.getRestrictedUntil().isAfter(now);
        
        // [시니어 조치] 제재 시간이 지났다면 DB 필드를 null로 초기화하고 상태 복구
        if (!restricted) {
            user.setRestrictedUntil(null);
            if (user.getStatus() == UserStatus.RESTRICTED) {
                user.setStatus(UserStatus.ACTIVE);
            }
            log.info("[KARMA] Restriction expired. User ID: {} status restored to ACTIVE.", user.getId());
        }
        
        return restricted;
    }
}
