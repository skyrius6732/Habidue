package com.habidue.app.service.user;

import com.habidue.app.domain.user.ExpHistory;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.domain.user.User;
import com.habidue.app.repository.user.ExpHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
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
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("EXP 부여 실패: 사용자를 찾을 수 없습니다. (ID: {})", userId);
            return;
        }

        int expToGrant = reason.getDefaultExp();
        
        // 1. 경험치 이력 저장
        ExpHistory history = ExpHistory.create(user, expToGrant, reason, description);
        expHistoryRepository.save(history);

        // 2. 유저 누적 경험치 업데이트
        user.setTotalExp(user.getTotalExp() + expToGrant);

        // 3. 레벨업 체크
        int oldLevel = user.getLevel();
        int newLevel = calculateLevel(user.getTotalExp());

        if (newLevel > oldLevel) {
            user.setLevel(newLevel);
            log.info("사용자 레벨업! ID: {}, {} -> {}", userId, oldLevel, newLevel);
            // TODO: 레벨업 축하 알림(Notification) 로직 추가 가능
        }

        userRepository.save(user); // 변경 감지(Dirty Checking)로 업데이트 되지만 명시적 저장
        
        // [시니어 조치] 경험치 변동 시 랭킹 캐시 즉시 무효화 (실시간 반영)
        rankingService.clearRankingCache();
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
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        int expToRevoke = reason.getDefaultExp();
        
        // 1. 경험치 회수 이력 저장 (음수 값)
        ExpHistory history = ExpHistory.create(user, -expToRevoke, reason, "[회수] " + description);
        expHistoryRepository.save(history);

        // 2. 유저 누적 경험치 차감 (최소 0 유지)
        user.setTotalExp(Math.max(0, user.getTotalExp() - expToRevoke));

        // 3. 레벨 재계산 (레벨 다운 가능)
        int oldLevel = user.getLevel();
        int newLevel = calculateLevel(user.getTotalExp());

        if (newLevel < oldLevel) {
            user.setLevel(newLevel);
            log.info("사용자 레벨 다운... ID: {}, {} -> {}", userId, oldLevel, newLevel);
        }

        userRepository.save(user);
        
        // [시니어 조치] 경험치 변동 시 랭킹 캐시 즉시 무효화 (실시간 반영)
        rankingService.clearRankingCache();
    }
}
