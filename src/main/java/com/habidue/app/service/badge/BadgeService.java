package com.habidue.app.service.badge;

import com.habidue.app.domain.badge.Badge;
import com.habidue.app.domain.badge.BadgeLevelRule;
import com.habidue.app.domain.badge.UserBadge;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.repository.badge.BadgeRepository;
import com.habidue.app.repository.badge.BadgeLevelRuleRepository;
import com.habidue.app.repository.badge.UserBadgeRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.habidue.app.service.user.ExpService;
import com.habidue.app.domain.user.ExpReason;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final BadgeLevelRuleRepository badgeLevelRuleRepository;
    private final ExpService expService; // [시니어 조치] EXP 연동

    /**
     * 활동 통계를 기반으로 모든 배지의 레벨을 체크하고 수여/갱신함
     */
    @Transactional
    public void checkAndAwardBadges(UserActivityStats stats) {
        // [시니어 조치] 받은 좋아요는 게시글과 댓글 합산 수치로 배지 판정
        int totalLikes = stats.getPostLikeReceivedCount() + stats.getCommentLikeReceivedCount();
        processBadge(stats, "KNOWLEDGE", totalLikes);
        
        processBadge(stats, "COLLECTOR", stats.getTotalNoticeInterestCount());
        processBadge(stats, "REVIEW", stats.getReviewPostCount());
        processBadge(stats, "COMMUNITY", stats.getTotalPostCount());
        processBadge(stats, "COMMUNICATOR", stats.getTotalCommentCount());

        // [시니어 조치] 출석 포인트 = 누적 출석 일수 + 현재 연속 출석 일수 (연속 출석 가중치 부여)
        int attendancePoints = stats.getTotalAttendanceCount() + stats.getConsecutiveAttendanceDays();
        processBadge(stats, "ATTENDANCE", attendancePoints);
    }

    private void processBadge(UserActivityStats stats, String type, int currentValue) {
        if (currentValue <= 0) return;

        // [시니어 조치] DB에서 현재 수치에 달성 가능한 최상위 레벨 룰 조회
        BadgeLevelRule targetRule = badgeLevelRuleRepository.findTopByBadgeTypeAndRequiredValueLessThanEqualOrderByLevelDesc(type, currentValue);
        if (targetRule == null) return;

        badgeRepository.findByCode(type + "_BASE").ifPresent(badge -> {
            UserBadge userBadge = userBadgeRepository.findByUserAndBadge(stats.getUser(), badge)
                    .orElseGet(() -> {
                        UserBadge ub = UserBadge.builder()
                                .user(stats.getUser())
                                .badge(badge)
                                .level(targetRule.getLevel())
                                .build();
                        UserBadge savedUb = userBadgeRepository.save(ub);
                        
                        // [시니어 조치] 최초 배지 획득 시 보너스 경험치 부여
                        expService.grantExp(stats.getUser().getId(), ExpReason.BADGE_ACQUIRED, "신규 배지 획득: " + targetRule.getRankTitle() + " " + targetRule.getCategoryName());
                        
                        return savedUb;
                    });

            // 승급이 필요한 경우 레벨만 갱신
            if (userBadge.getLevel() < targetRule.getLevel()) {
                userBadge.updateLevel(targetRule.getLevel(), null); // displayName에 null 전달
                userBadgeRepository.saveAndFlush(userBadge);
                
                // [시니어 조치] 배지 레벨업 시 보너스 경험치 부여
                expService.grantExp(stats.getUser().getId(), ExpReason.BADGE_ACQUIRED, "배지 승급: " + targetRule.getRankTitle() + " " + targetRule.getCategoryName());
                
                log.info("🎖️ 유저 [{}]님의 [{}] 배지가 Lv.{}로 승급했습니다!", 
                        stats.getUser().getNickname(), type, targetRule.getLevel());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Badge> getAllMasters() {
        return badgeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<com.habidue.app.dto.badge.BadgeResponseDto> getMyBadgeDtos(User user) {
        UserActivityStats stats = userActivityStatsRepository.findById(user.getId())
                .orElseGet(() -> UserActivityStats.createEmpty(user));

        // 모든 배지 레벨 규칙 로드 (N+1 방지)
        List<BadgeLevelRule> allRules = badgeLevelRuleRepository.findAllByOrderByLevelAsc();
        
        // 규칙들을 타입_레벨 키로 맵핑
        java.util.Map<String, BadgeLevelRule> ruleMap = allRules.stream()
                .collect(java.util.stream.Collectors.toMap(
                    r -> r.getBadgeType() + "_" + r.getLevel(),
                    r -> r
                ));

        return userBadgeRepository.findByUserOrderByAcquiredAtDesc(user).stream()
                .map(ub -> {
                    String badgeType = ub.getBadge().getCode().replace("_BASE", "");
                    
                    // 1. 실시간 이름 조합
                    BadgeLevelRule currentRule = ruleMap.get(badgeType + "_" + ub.getLevel());
                    String realName = (currentRule != null) ? currentRule.getFullDisplayName() : ub.getBadge().getName();
                    
                    // 2. 현재 수치(Raw Value) 파악
                    int curVal = getStatValueByType(stats, badgeType);
                    
                    // 3. 다음 목표 수치 파악
                    // 같은 타입 중 현재 레벨보다 높은 가장 작은 레벨의 룰 찾기
                    BadgeLevelRule nextRule = allRules.stream()
                        .filter(r -> r.getBadgeType().equals(badgeType) && r.getLevel() > ub.getLevel())
                        .findFirst() // OrderByLevelAsc 덕분에 첫 번째가 다음 레벨
                        .orElse(null);
                        
                    Integer nextTarget = (nextRule != null) ? nextRule.getRequiredValue() : null;

                    return com.habidue.app.dto.badge.BadgeResponseDto.from(ub, realName, curVal, nextTarget);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private int getStatValueByType(UserActivityStats stats, String type) {
        return switch (type) {
            case "KNOWLEDGE" -> stats.getPostLikeReceivedCount() + stats.getCommentLikeReceivedCount();
            case "COLLECTOR" -> stats.getTotalNoticeInterestCount();
            case "REVIEW" -> stats.getReviewPostCount();
            case "COMMUNITY" -> stats.getTotalPostCount();
            case "COMMUNICATOR" -> stats.getTotalCommentCount();
            case "ATTENDANCE" -> stats.getTotalAttendanceCount() + stats.getConsecutiveAttendanceDays();
            default -> 0;
        };
    }

    @Transactional(readOnly = true)
    public List<UserBadge> getMyBadges(User user) {
        return userBadgeRepository.findByUserOrderByAcquiredAtDesc(user);
    }

    /**
     * [관리자 전용] 모든 배지 규칙 조회
     */
    @Transactional(readOnly = true)
    public List<BadgeLevelRule> getAllRules() {
        return badgeLevelRuleRepository.findAllByOrderByLevelAsc();
    }

    @Transactional
    public void updateRule(Long ruleId, com.habidue.app.dto.admin.BadgeLevelRuleRequestDto requestDto) {
        BadgeLevelRule rule = badgeLevelRuleRepository.findById(ruleId)
                .orElseThrow(() -> new java.util.NoSuchElementException("규칙을 찾을 수 없습니다."));
        
        rule.update(
            requestDto.getRequiredValue(),
            requestDto.getRankEmoji(),
            requestDto.getRankTitle(),
            requestDto.getCategoryName()
        );
    }

    /**
     * [관리자 전용] 새 배지 레벨 규칙 생성
     */
    @Transactional
    public void createRule(String type, com.habidue.app.dto.admin.BadgeLevelRuleRequestDto requestDto, int level) {
        BadgeLevelRule rule = BadgeLevelRule.builder()
                .badgeType(type)
                .level(level)
                .requiredValue(requestDto.getRequiredValue())
                .rankEmoji(requestDto.getRankEmoji())
                .rankTitle(requestDto.getRankTitle())
                .categoryName(requestDto.getCategoryName())
                .build();
        badgeLevelRuleRepository.save(rule);
    }

    /**
     * [관리자 전용] 배지 규칙 삭제
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        badgeLevelRuleRepository.deleteById(ruleId);
    }

    /**
     * [관리자 전용] 새 베이스 배지 생성
     */
    @Transactional
    public void createBadgeMaster(String code, String name, String description, String type, String badgeTip) {
        if (badgeRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 배지 코드입니다.");
        }
        Badge badge = Badge.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .badgeTip(badgeTip)
                .level(1)
                .build();
        badgeRepository.save(badge);
    }

    /**
     * [관리자 전용] 배지 마스터 및 관련 규칙 삭제
     */
    @Transactional
    public void deleteBadgeMaster(String code) {
        Badge badge = badgeRepository.findByCode(code)
                .orElseThrow(() -> new java.util.NoSuchElementException("배지 마스터를 찾을 수 없습니다."));
        
        // 연관된 규칙들 삭제 (마스터 코드에서 _BASE를 뺀 값이 규칙의 badgeType)
        String typePrefix = code.replace("_BASE", "");
        badgeLevelRuleRepository.deleteByBadgeType(typePrefix);
        
        // 마스터 배지 삭제
        badgeRepository.delete(badge);
    }

    @Transactional
    public void syncAllUsersBadges() {
        List<UserActivityStats> allStats = userActivityStatsRepository.findAll();
        for (UserActivityStats stats : allStats) {
            checkAndAwardBadges(stats);
        }
        log.info("📢 전체 유저 배지 재동기화 완료 (대상: {}명)", allStats.size());
    }
}
