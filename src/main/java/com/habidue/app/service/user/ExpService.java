package com.habidue.app.service.user;

import com.habidue.app.domain.user.ExpHistory;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.repository.user.ExpHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpService {

    private final UserRepository userRepository;
    private final ExpHistoryRepository expHistoryRepository;
    private final com.habidue.app.service.ranking.RankingService rankingService;
    private final UserEffectService userEffectService;
    private final NotificationService notificationService;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * 레벨 테이블 (간단한 산술식 또는 고정 구간)
     * 예: Level N에 도달하기 위한 누적 EXP = N * N * 50
     */
    private int calculateLevel(long totalExp) {
        // 간단한 루트 계산 기반 레벨링 (기본값 1)
        int calculatedLevel = (int) Math.floor(Math.sqrt(totalExp / 50.0)) + 1;
        return Math.min(calculatedLevel, 100); // 최고 레벨 100 제한
    }

    /**
     * 사용자에게 경험치를 부여하고 레벨업 여부를 판단하는 핵심 메서드
     * (메인 트랜잭션과 독립적으로 처리하기 위해 Async 적용 고려 가능. 현재는 동기식 유지)
     */
    @Transactional
    public void grantExp(Long userId, ExpReason reason, String description) {
        // 1. 원자적 경험치 업데이트 수행
        int expToGrant = reason.getDefaultExp();
        userRepository.updateExp(userId, expToGrant);

        // 2. 이력 저장을 위한 유저 조회 (업데이트된 값 포함)
        User user = userRepository.findById(userId).orElseThrow();
        // [시니어 조치] 레벨 계산 전 유저 경험치 상태를 DB와 강제 동기화 (프록시 오염 방지)
        entityManager.refresh(user);
        
        // 3. 경험치 이력 저장
        ExpHistory history = ExpHistory.create(user, expToGrant, reason, description);
        expHistoryRepository.save(history);

        // 4. 레벨업 체크 및 업데이트
        int oldLevel = user.getLevel();
        int newLevel = calculateLevel(user.getTotalExp());

        if (newLevel > oldLevel) {
            userRepository.updateLevel(userId, newLevel);
            log.info("사용자 레벨업! ID: {}, {} -> {}", userId, oldLevel, newLevel);

            // 레벨업 알림 전송
            notificationService.send(user, NotificationType.LEVEL_UP,
                String.format("축하합니다! 레벨 %d에 도달했습니다.", newLevel), null, null);

            // [시니어 조치] 레벨업 시 해금되는 이펙트 자동 지급
            grantEffectsForLevel(userId, oldLevel, newLevel);
        }

        // [시니어 조치] 경험치 변동 시 랭킹 캐시 즉시 무효화 (실시간 반영)
        rankingService.clearRankingCache();
    }

    /**
     * [시니어 조치] 레벨업 시 해금되는 이펙트를 자동으로 지급
     */
    private void grantEffectsForLevel(Long userId, int oldLevel, int newLevel) {
        // 레벨별 이펙트 맵
        java.util.Map<Integer, String> levelUnlocks = java.util.Map.ofEntries(
            java.util.Map.entry(5, "MAGIC_BUBBLES"),
            java.util.Map.entry(10, "BRONZE_WINGS"),
            java.util.Map.entry(15, "STARRY_NIGHT"),
            java.util.Map.entry(20, "SILVER_WINGS"),
            java.util.Map.entry(25, "BOMB"),
            java.util.Map.entry(30, "GOLD_WINGS"),
            java.util.Map.entry(35, "SHADOW_DEMON"),
            java.util.Map.entry(40, "VOID_RIFT"),
            java.util.Map.entry(45, "THUNDER_BLUE"),
            java.util.Map.entry(50, "NEON_SIGN"),
            java.util.Map.entry(60, "AURORA_FLAME"),
            java.util.Map.entry(70, "RAINBOW_WAVE")
        );

        User user = userRepository.findById(userId).orElse(null);

        // oldLevel과 newLevel 사이에 해금되는 이펙트들을 지급
        for (java.util.Map.Entry<Integer, String> entry : levelUnlocks.entrySet()) {
            int unlockLevel = entry.getKey();
            String effectCode = entry.getValue();

            // 이전 레벨에서는 해금되지 않았는데, 새 레벨에서 해금되는 경우
            if (oldLevel < unlockLevel && newLevel >= unlockLevel) {
                try {
                    userEffectService.grantEffect(userId, effectCode, com.habidue.app.domain.user.UserEffect.EffectSource.LEVEL);
                    log.info("레벨업 이펙트 지급: userId={}, effectCode={}, level={}", userId, effectCode, unlockLevel);

                    // 이펙트 획득 알림 전송
                    if (user != null) {
                        notificationService.send(user, NotificationType.EFFECT_ACQUIRED,
                            String.format("✨ 새로운 이펙트를 획득했습니다!"), null, null);
                    }
                } catch (Exception e) {
                    log.warn("레벨업 이펙트 지급 실패: userId={}, effectCode={}, error={}", userId, effectCode, e.getMessage());
                }
            }
        }
    }

    /**
     * [시니어 조치] 특정 활동이 삭제될 때 부여됐던 경험치를 회수함
     * 게시글/댓글 삭제 시 호출되어 어뷰징 방지 및 데이터 정합성 유지
     */
    @Transactional
    public void revokeExp(Long userId, ExpReason reason, String description) {
        if (userId == null || userId <= 0) {
            log.warn("EXP 회수 건너뜀: 유효하지 않은 사용자 ID (ID: {})", userId);
            return;
        }

        // 1. 원자적 경험치 차감 수행 (음수 전달)
        int expToRevoke = reason.getDefaultExp();
        userRepository.updateExp(userId, -expToRevoke);

        // 2. 이력 저장을 위한 유저 조회
        User user = userRepository.findById(userId).orElseThrow();
        // [시니어 조치] 레벨 재계산 전 유저 경험치 상태를 DB와 강제 동기화
        entityManager.refresh(user);

        // 3. 경험치 이력 저장 (차감 내역)
        ExpHistory history = ExpHistory.create(user, -expToRevoke, reason, "[회수] " + description);
        expHistoryRepository.save(history);

        // 4. 레벨 재계산 및 업데이트 (레벨 다운 가능)
        int oldLevel = user.getLevel();
        int newLevel = calculateLevel(user.getTotalExp());

        if (newLevel < oldLevel) {
            userRepository.updateLevel(userId, newLevel);
            log.info("사용자 레벨 다운... ID: {}, {} -> {}", userId, oldLevel, newLevel);
        }

        // [시니어 조치] 경험치 변동 시 랭킹 캐시 즉시 무효화 (실시간 반영)
        rankingService.clearRankingCache();
    }
}
