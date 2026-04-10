package com.habidue.app.service.ranking;

import com.habidue.app.domain.user.ExpCategory;
import com.habidue.app.domain.user.ExpReason;
import com.habidue.app.dto.ranking.RankerResponseDto;
import com.habidue.app.repository.user.ExpHistoryRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.ExpHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import com.habidue.app.dto.ranking.HotNoticeResponseDto;
import com.habidue.app.repository.notice.NoticeRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
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
    private final NoticeRepository noticeRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final String RANKING_CACHE_PREFIX = "ranking:cache:v3:";
    private static final String RANKING_UPDATED_AT_PREFIX = "ranking:updated_at:";
    private static final String HOT_NOTICE_KEY = "ranking:hot:notices";
    private static final String HOT_NOTICE_UPDATE_KEY = "ranking:hot:updated_at";
    private static final String VIEW_LOCK_PREFIX = "ranking:view:lock:";
    private static final String RAW_VIEW_COUNT_PREFIX = "ranking:view:raw_count:";

    // [가중치 및 마일스톤 보너스 상수]
    public static final double SCORE_VIEW = 1.0;          // 단순 조회
    public static final double SCORE_INTEREST = 10.0;     // 관심 등록(찜)
    public static final double SCORE_POST = 30.0;         // 소통방 게시글 작성
    public static final double SCORE_COMMENT = 10.0;      // 댓글/답글 작성
    public static final double SCORE_BOARD_UNLOCK = 100.0; // 소통방 최초 해금 (10명 달성)
    public static final double SCORE_BOARD_REVIVE = 50.0;  // 휴면 소통방 깨우기 성공

    /**
     * 공고의 실시간 점수를 증가시킵니다. (차감 시 최소값 0 보장)
     */
    public void increaseNoticeScore(Long noticeId, double score) {
        try {
            Double newScore = redisTemplate.opsForZSet().incrementScore(HOT_NOTICE_KEY, String.valueOf(noticeId), score);
            if (newScore != null && newScore < 0) {
                redisTemplate.opsForZSet().add(HOT_NOTICE_KEY, String.valueOf(noticeId), 0.0);
            }
            redisTemplate.opsForValue().set(HOT_NOTICE_UPDATE_KEY, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            log.warn("Failed to update hot notice score: {}", e.getMessage());
        }
    }

    /**
     * 조회수 어뷰징을 방지하며 점수를 올립니다. (1시간당 1회 인정)
     */
    public void increaseNoticeViewScore(Long noticeId, String identifier) {
        String lockKey = VIEW_LOCK_PREFIX + noticeId + ":" + identifier;
        String rawCountKey = RAW_VIEW_COUNT_PREFIX + noticeId;
        try {
            redisTemplate.opsForValue().increment(rawCountKey);
            Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(lockKey, "true", 1, java.util.concurrent.TimeUnit.HOURS);
            if (Boolean.TRUE.equals(isFirstView)) {
                increaseNoticeScore(noticeId, SCORE_VIEW);
            }
        } catch (Exception e) {
            log.warn("View scoring error: {}", e.getMessage());
        }
    }

    /**
     * 실시간 급상승 공고 TOP N을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<HotNoticeResponseDto> getHotNotices(int limit) {
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_NOTICE_KEY, 0, limit - 1);

        if (typedTuples == null || typedTuples.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<HotNoticeResponseDto> result = new java.util.ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            Long noticeId = Long.parseLong(tuple.getValue());
            double score = tuple.getScore() != null ? tuple.getScore() : 0.0;
            String rawViewStr = redisTemplate.opsForValue().get(RAW_VIEW_COUNT_PREFIX + noticeId);
            long viewCount = rawViewStr != null ? Long.parseLong(rawViewStr) : 0L;

            noticeRepository.findById(noticeId).ifPresent(notice -> {
                result.add(HotNoticeResponseDto.from(notice, score, result.size() + 1, viewCount));
            });
        }
        return result;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void clearHotNotices() {
        log.info("Cleaning up daily hot notice rankings...");
        redisTemplate.delete(HOT_NOTICE_KEY);
        redisTemplate.delete(HOT_NOTICE_UPDATE_KEY);
        Set<String> keys = redisTemplate.keys(RAW_VIEW_COUNT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public String getHotNoticeUpdatedAt() {
        return redisTemplate.opsForValue().get(HOT_NOTICE_UPDATE_KEY);
    }

    public void clearRankingCache() {
        try {
            Set<String> keys = redisTemplate.keys(RANKING_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[RANKING] Ranking cache cleared.");
            }
        } catch (Exception e) {
            log.warn("Failed to clear ranking cache: {}", e.getMessage());
        }
    }

    /**
     * 지정된 기간 및 분야의 상위 랭커 목록을 조회합니다. (Hydration 적용)
     */
    @Transactional(readOnly = true)
    public List<RankerResponseDto> getTopRankers(String period, String category, int limit) {
        String cacheKey = RANKING_CACHE_PREFIX + period + ":" + category + ":" + limit;
        String timeKey = RANKING_UPDATED_AT_PREFIX + period + ":" + category + ":" + limit;
        
        List<RankerResponseDto> baseRankers;

        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                baseRankers = objectMapper.readValue(cachedData, new com.fasterxml.jackson.core.type.TypeReference<List<RankerResponseDto>>() {});
            } else {
                baseRankers = calculateAndCacheRankers(period, category, limit, cacheKey, timeKey);
            }
        } catch (Exception e) {
            log.warn("Ranking Cache Error: {}", e.getMessage());
            baseRankers = calculateAndCacheRankers(period, category, limit, cacheKey, timeKey);
        }

        return hydrateRankerProfiles(baseRankers);
    }

    private List<RankerResponseDto> calculateAndCacheRankers(String period, String category, int limit, String cacheKey, String timeKey) {
        LocalDateTime startDate = calculateStartDate(period);
        List<ExpReason> reasons = getReasonsByCategory(category);
        Pageable pageable = PageRequest.of(0, limit);

        Page<ExpHistoryRepository.RankerProjection> currentRankers = 
                expHistoryRepository.findTopRankersByPeriodAndReasons(startDate, reasons, pageable);

        boolean isAllTime = "ALL".equalsIgnoreCase(period);
        List<Long> prevRankerIds = isAllTime ? java.util.Collections.emptyList() : 
                getPreviousRankerIds(calculatePreviousStartDate(period), startDate, reasons);

        List<RankerResponseDto> result = new java.util.ArrayList<>();
        for (int i = 0; i < currentRankers.getContent().size(); i++) {
            ExpHistoryRepository.RankerProjection p = currentRankers.getContent().get(i);
            result.add(RankerResponseDto.builder()
                    .userPublicId(p.getPublicId())
                    .nickname(p.getNickname())
                    .level(p.getLevel())
                    .exp(p.getTotalExp())
                    .karmaPoint(p.getKarmaPoint() != null ? p.getKarmaPoint() : 1000)
                    .rankDiff(isAllTime ? "0" : calculateRankDiff(i + 1, prevRankerIds.indexOf(p.getUserId()) + 1))
                    .equippedEffect(p.getEquippedEffect())
                    .showLevelEffects(p.isShowLevelEffects())
                    .showEquippedEffect(p.isShowEquippedEffect())
                    .equippedTier(p.getEquippedTier())
                    .build());
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 10, java.util.concurrent.TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(timeKey, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 10, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache ranking: {}", e.getMessage());
        }

        return result;
    }

    private List<RankerResponseDto> hydrateRankerProfiles(List<RankerResponseDto> baseRankers) {
        if (baseRankers == null || baseRankers.isEmpty()) return baseRankers;

        List<String> publicIds = baseRankers.stream().map(RankerResponseDto::getUserPublicId).collect(Collectors.toList());
        java.util.Map<String, User> userMap = userRepository.findAllByPublicIdIn(publicIds).stream()
                .collect(Collectors.toMap(User::getPublicId, u -> u));

        java.util.Map<String, com.habidue.app.domain.badge.BadgeLevelRule> ruleMap = badgeLevelRuleRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getBadgeType() + "_" + r.getLevel(), r -> r, (r1, r2) -> r1));
        java.util.Map<Long, com.habidue.app.domain.badge.Badge> badgeMasterMap = badgeRepository.findAll().stream()
                .collect(Collectors.toMap(com.habidue.app.domain.badge.Badge::getId, b -> b));
        java.util.Map<String, Integer> userBadgeLevelMap = new java.util.HashMap<>();
        List<Long> internalUserIds = userMap.values().stream().map(User::getId).collect(Collectors.toList());
        userBadgeRepository.findAllByUserIds(internalUserIds)
                .forEach(ub -> userBadgeLevelMap.put(ub.getUser().getId() + "_" + ub.getBadge().getId(), ub.getLevel()));

        for (RankerResponseDto dto : baseRankers) {
            User user = userMap.get(dto.getUserPublicId());
            if (user != null) {
                dto.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                dto.setLevel(user.getLevel());
                dto.setKarmaPoint(user.getKarmaPoint());
                dto.setEquippedTier(user.getEquippedTier());
                dto.setEquippedEffect(user.getEquippedEffect());
                dto.setShowLevelEffects(user.isShowLevelEffects());
                dto.setShowEquippedEffect(user.isShowEquippedEffect());

                if (user.getEquippedBadgeId() != null) {
                    com.habidue.app.domain.badge.Badge badgeMaster = badgeMasterMap.get(user.getEquippedBadgeId());
                    if (badgeMaster != null) {
                        Integer level = userBadgeLevelMap.get(user.getId() + "_" + user.getEquippedBadgeId());
                        if (level != null) {
                            String badgeType = badgeMaster.getCode().replace("_BASE", "");
                            com.habidue.app.domain.badge.BadgeLevelRule rule = ruleMap.get(badgeType + "_" + level);
                            dto.setEquippedBadgeName((rule != null) ? rule.getFullDisplayName() : badgeMaster.getName());
                        } else {
                            dto.setEquippedBadgeName(badgeMaster.getName());
                        }
                    }
                } else {
                    dto.setEquippedBadgeName(null);
                }
            }
        }
        return baseRankers;
    }

    public String getRankingUpdatedAt(String period, String category, int limit) {
        String timeKey = RANKING_UPDATED_AT_PREFIX + period + ":" + category + ":" + limit;
        return redisTemplate.opsForValue().get(timeKey);
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
            default -> start.minusYears(1);
        };
    }

    private List<Long> getPreviousRankerIds(LocalDateTime start, LocalDateTime end, List<ExpReason> reasons) {
        PageRequest pageable = PageRequest.of(0, 200);
        return expHistoryRepository.findTopRankerIdsByPeriodRange(startDateFix(start), end, reasons, pageable);
    }

    private LocalDateTime startDateFix(LocalDateTime start) {
        return start;
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
