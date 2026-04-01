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

    private static final String RANKING_CACHE_PREFIX = "ranking:cache:v2:";
    private static final String HOT_NOTICE_KEY = "ranking:hot:notices";
    private static final String HOT_NOTICE_UPDATE_KEY = "ranking:hot:updated_at";
    private static final String VIEW_LOCK_PREFIX = "ranking:view:lock:";
    private static final String RAW_VIEW_COUNT_PREFIX = "ranking:view:raw_count:";

    // [가중치 및 마일스톤 보너스 상수]
    public static final double SCORE_VIEW = 1.0;          // 단순 조회
    public static final double SCORE_INTEREST = 20.0;     // 관심 등록(찜)
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
            // [시니어 조치] 순수 조회수는 별도 카운팅하여 툴팁에 노출
            redisTemplate.opsForValue().increment(rawCountKey);

            // [시니어 조치] 1시간 동안 해당 유저/IP의 조회 점수 합산 차단
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
            
            // [시니어 조치] Redis에서 순수 조회수 가져오기
            String rawViewStr = redisTemplate.opsForValue().get(RAW_VIEW_COUNT_PREFIX + noticeId);
            long viewCount = rawViewStr != null ? Long.parseLong(rawViewStr) : 0L;

            noticeRepository.findById(noticeId).ifPresent(notice -> {
                result.add(HotNoticeResponseDto.from(notice, score, result.size() + 1, viewCount));
            });
        }
        return result;
    }

    /**
     * 매일 자정에 급상승 랭킹을 초기화합니다. (Daily Hot Ranking)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void clearHotNotices() {
        log.info("Cleaning up daily hot notice rankings...");
        redisTemplate.delete(HOT_NOTICE_KEY);
        redisTemplate.delete(HOT_NOTICE_UPDATE_KEY);
        
        // [시니어 조치] 개별 조회수 키들도 모두 삭제
        Set<String> keys = redisTemplate.keys(RAW_VIEW_COUNT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 마지막 업데이트 시각을 조회합니다.
     */
    public String getHotNoticeUpdatedAt() {
        return redisTemplate.opsForValue().get(HOT_NOTICE_UPDATE_KEY);
    }

    /**
     * [시니어 조치] 경험치 변동 시 관련 랭킹 캐시를 모두 삭제하여 실시간성을 보장함
     */
    public void clearRankingCache() {
        try {
            Set<String> keys = redisTemplate.keys(RANKING_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[RANKING] Ranking cache cleared for real-time update.");
            }
        } catch (Exception e) {
            log.warn("Failed to clear ranking cache: {}", e.getMessage());
        }
    }

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
