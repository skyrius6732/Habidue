package com.habidue.app.service.ranking;

import com.habidue.app.domain.user.ExpCategory;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.dto.ranking.RankerResponseDto;
import com.habidue.app.repository.user.ExpHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final ExpHistoryRepository expHistoryRepository;
    private final UserRepository userRepository;
    private final com.habidue.app.repository.badge.UserBadgeRepository userBadgeRepository;
    private final com.habidue.app.repository.badge.BadgeRepository badgeRepository;
    private final com.habidue.app.repository.badge.BadgeLevelRuleRepository badgeLevelRuleRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final String RANKING_CACHE_PREFIX = "ranking:cache:v2:";

    /**
     * 지정된 기간 및 분야의 상위 랭커 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<RankerResponseDto> getTopRankers(String period, String category, int limit) {
        String cacheKey = RANKING_CACHE_PREFIX + period + ":" + category + ":" + limit;
        
        // 1. Redis 캐시 확인
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, new com.fasterxml.jackson.core.type.TypeReference<List<RankerResponseDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Ranking Cache Read Error: {}", e.getMessage());
        }

        // 2. 캐시 없으면 DB 조회 및 계산
        LocalDateTime startDate = calculateStartDate(period);
        List<ExpReason> reasons = getReasonsByCategory(category);
        Pageable pageable = PageRequest.of(0, limit);

        Page<ExpHistoryRepository.RankerProjection> currentRankers = 
                expHistoryRepository.findTopRankersByPeriodAndReasons(startDate, reasons, pageable);

        boolean isAllTime = "ALL".equalsIgnoreCase(period);
        List<Long> prevRankerIds = isAllTime ? java.util.Collections.emptyList() : 
                getPreviousRankerIds(calculatePreviousStartDate(period), startDate, reasons);

        // 성능 최적화: 배지 규칙 전체 로드
        java.util.Map<String, com.habidue.app.domain.badge.BadgeLevelRule> ruleMap = badgeLevelRuleRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.getBadgeType() + "_" + r.getLevel(), r -> r, (r1, r2) -> r1));

        // 성능 최적화: 배지 마스터 전체 로드
        java.util.Map<Long, com.habidue.app.domain.badge.Badge> badgeMasterMap = badgeRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(com.habidue.app.domain.badge.Badge::getId, b -> b));

        // 성능 최적화: 유저들의 배지 획득 레벨 정보 일괄 조회
        java.util.List<Long> userIds = currentRankers.getContent().stream().map(ExpHistoryRepository.RankerProjection::getUserId).toList();
        java.util.Map<String, Integer> userBadgeLevelMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userBadgeRepository.findAllByUserIds(userIds)
                .forEach(ub -> userBadgeLevelMap.put(ub.getUser().getId() + "_" + ub.getBadge().getId(), ub.getLevel()));
        }

        List<RankerResponseDto> result = new java.util.ArrayList<>();
        for (int i = 0; i < currentRankers.getContent().size(); i++) {
            ExpHistoryRepository.RankerProjection p = currentRankers.getContent().get(i);
            
            // resolveBadgeName 인라인화 및 최적화
            String badgeName = null;
            if (p.getEquippedBadgeId() != null) {
                com.habidue.app.domain.badge.Badge badgeMaster = badgeMasterMap.get(p.getEquippedBadgeId());
                if (badgeMaster != null) {
                    Integer level = userBadgeLevelMap.get(p.getUserId() + "_" + p.getEquippedBadgeId());
                    if (level != null) {
                        String badgeType = badgeMaster.getCode().replace("_BASE", "");
                        com.habidue.app.domain.badge.BadgeLevelRule rule = ruleMap.get(badgeType + "_" + level);
                        badgeName = (rule != null) ? rule.getFullDisplayName() : badgeMaster.getName();
                    } else {
                        badgeName = badgeMaster.getName();
                    }
                }
            }

            result.add(RankerResponseDto.builder()
                    .userId(p.getUserId())
                    .nickname(p.getNickname())
                    .level(p.getLevel())
                    .exp(p.getTotalExp())
                    .karmaPoint(p.getKarmaPoint() != null ? p.getKarmaPoint() : 1000)
                    .rankDiff(isAllTime ? "0" : calculateRankDiff(i + 1, prevRankerIds.indexOf(p.getUserId()) + 1))
                    .equippedBadgeName(badgeName)
                    .build());
        }

        // 3. Redis에 캐시 저장 (10분간 유효)
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 10, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Ranking Cache Write Error: {}", e.getMessage());
        }

        return result;
    }

    private String resolveBadgeName(ExpHistoryRepository.RankerProjection p, java.util.Map<String, com.habidue.app.domain.badge.BadgeLevelRule> ruleMap) {
        if (p.getEquippedBadgeId() == null) return null;
        return badgeRepository.findById(p.getEquippedBadgeId())
            .map(badgeMaster -> {
                String badgeType = badgeMaster.getCode().replace("_BASE", "");
                return userBadgeRepository.findByUserIdAndBadgeId(p.getUserId(), p.getEquippedBadgeId())
                    .map(ub -> {
                        com.habidue.app.domain.badge.BadgeLevelRule rule = ruleMap.get(badgeType + "_" + ub.getLevel());
                        return (rule != null) ? rule.getFullDisplayName() : badgeMaster.getName();
                    }).orElse(badgeMaster.getName());
            }).orElse(null);
    }

    private List<ExpReason> getReasonsByCategory(String category) {
        if (category == null || "TOTAL".equalsIgnoreCase(category)) return java.util.Arrays.asList(ExpReason.values());
        
        ExpCategory expCat = ExpCategory.valueOf(category.toUpperCase());
        return java.util.Arrays.stream(ExpReason.values())
                .filter(r -> r.getCategory() == expCat)
                .collect(Collectors.toList());
    }

    private LocalDateTime calculatePreviousStartDate(String period) {
        LocalDateTime start = calculateStartDate(period);
        return switch (period.toUpperCase()) {
            case "DAILY" -> start.minusDays(1);
            case "WEEKLY" -> start.minusWeeks(1);
            case "MONTHLY" -> start.minusMonths(1);
            default -> start.minusYears(1); // ALL일 경우 의미는 없으나 형식상 추가
        };
    }

    private List<Long> getPreviousRankerIds(LocalDateTime start, LocalDateTime end, List<ExpReason> reasons) {
        // [시니어 조치] 이전 주기의 TOP 200까지의 ID 리스트를 가져와서 변동 폭 계산 기반 마련
        PageRequest pageable = PageRequest.of(0, 200);
        
        // 특정 기간(이전 주기)의 랭킹을 가져오는 쿼리 (끝나는 시간 조건 추가 필요)
        // 여기서는 편의상 ExpHistoryRepository에 기간 범위를 받는 쿼리가 추가되었다고 가정하거나 새로 정의
        return expHistoryRepository.findTopRankerIdsByPeriodRange(start, end, reasons, pageable);
    }

    private String calculateRankDiff(int current, int prev) {
        if (prev <= 0) return "NEW";
        int diff = prev - current;
        if (diff > 0) return "+" + diff;
        if (diff < 0) return String.valueOf(diff);
        return "0";
    }

    private LocalDateTime calculateStartDate(String period) {
        LocalDate now = LocalDate.now();
        return switch (period.toUpperCase()) {
            case "DAILY" -> now.atStartOfDay(); 
            case "WEEKLY" -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(); 
            case "MONTHLY" -> now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(); 
            case "ALL" -> LocalDateTime.of(2000, 1, 1, 0, 0);
            default -> now.minusDays(1).atStartOfDay();
        };
    }
}
