package com.habidue.app.service.user;

import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.user.UserStatus;
import com.habidue.app.dto.SignupRequest;
import com.habidue.app.dto.user.UserActivityResponseDto;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.repository.badge.UserBadgeRepository;
import com.habidue.app.service.badge.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final com.habidue.app.repository.badge.BadgeLevelRuleRepository badgeLevelRuleRepository; // [시니어 조치] 규칙 저장소 추가
    private final BadgeService badgeService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final String BADGE_RULES_CACHE_KEY = "badge:rules:all";

    public static final String BLOCKED_USER_PREFIX = "user:blocked:";
    public static final String ONLINE_USER_PREFIX = "user:online:";

    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setNickname(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        // [시니어 조치] 유저 통계 엔티티 생성 (배지 시스템용)
        userActivityStatsRepository.save(UserActivityStats.createEmpty(savedUser));

        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserActivityResponseDto getUserActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        UserActivityStats stats = userActivityStatsRepository.findById(userId)
                .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(user)));

        // [시니어 조치] BadgeService의 공통 로직을 사용하여 실시간 이름 및 진행도 포함된 배지 리스트 조회
        List<com.habidue.app.dto.badge.BadgeResponseDto> badges = badgeService.getMyBadgeDtos(user);

        // [시니어 조치] 모든 배지 레벨 규칙 조회 (Redis 캐싱 적용으로 성능 극대화)
        List<com.habidue.app.dto.badge.BadgeLevelRuleResponseDto> badgeRules = null;
        try {
            String cachedRules = redisTemplate.opsForValue().get(BADGE_RULES_CACHE_KEY);
            if (cachedRules != null) {
                badgeRules = objectMapper.readValue(cachedRules, new com.fasterxml.jackson.core.type.TypeReference<List<com.habidue.app.dto.badge.BadgeLevelRuleResponseDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Badge Rules Cache Read Error: {}", e.getMessage());
        }

        if (badgeRules == null) {
            badgeRules = badgeLevelRuleRepository.findAll().stream()
                    .map(com.habidue.app.dto.badge.BadgeLevelRuleResponseDto::from)
                    .collect(Collectors.toList());
            
            // 캐시 저장 (1시간 유효)
            try {
                redisTemplate.opsForValue().set(BADGE_RULES_CACHE_KEY, objectMapper.writeValueAsString(badgeRules), 1, java.util.concurrent.TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Badge Rules Cache Write Error: {}", e.getMessage());
            }
        }

        // [신규] 순위 정보 계산
        long totalRank = userRepository.calculateRankByTotalExp(user.getTotalExp());
        long totalUserCount = userRepository.count();

        return UserActivityResponseDto.of(stats, badges, badgeRules, totalRank, totalUserCount);
    }
@Transactional
public void syncUserActivity(Long userId) {
    UserActivityStats stats = userActivityStatsRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("활동 통계 정보가 없습니다."));

    // [시니어 조치] 주입된 배지 서비스를 통해 현재 통계 기반으로 배지 재검고 및 수여
    badgeService.checkAndAwardBadges(stats);
}

    @Transactional(readOnly = true)
    public User getUserByUsername(String identifier) {
        // 이메일로 먼저 찾고, 없으면 유저네임으로 찾음
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + identifier));
    }

    @Transactional
    public User updateNickname(Long userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        if (user.getNickname() != null && user.getNickname().equals(newNickname)) {
            return user;
        }

        if (newNickname == null || !newNickname.matches("^[a-zA-Z0-9가-힣]{2,12}$")) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자 조합의 2~12자여야 합니다. (특수문자 제외)");
        }

        if (userRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        user.setNickname(newNickname);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    /**
     * [시니어 조치] 대표 배지(칭호) 장착/해제
     */
    @Transactional
    public User updateEquippedBadge(Long userId, Long badgeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        if (badgeId != null) {
            // 해당 유저가 실제로 해당 배지를 보유하고 있는지 검증
            boolean hasBadge = userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId);
            if (!hasBadge) {
                throw new IllegalArgumentException("보유하지 않은 배지는 장착할 수 없습니다.");
            }
        }
        
        user.setEquippedBadgeId(badgeId);
        return userRepository.save(user);
    }

    // --- [관리자 전용 기능] ---

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(Long userId, UserStatus status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        user.setStatus(status);
        if (status == UserStatus.BLOCKED) {
            String finalReason = (reason != null && !reason.trim().isEmpty()) ? reason : "관리자에 의해 계정이 차단되었습니다.";
            user.setBlockedReason(finalReason);
            user.setBlockedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(BLOCKED_USER_PREFIX + userId, finalReason);
        } else {
            user.setBlockedReason(null);
            user.setBlockedAt(null);
            redisTemplate.delete(BLOCKED_USER_PREFIX + userId);
        }
        
        return userRepository.save(user);
    }

    // --- [실시간 접속 관리 (Redis)] ---

    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_USER_PREFIX + userId));
    }

    public void updateUserOnlineStatus(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_USER_PREFIX + userId, "true", 5, TimeUnit.MINUTES);
    }

    public void setUserOffline(Long userId) {
        redisTemplate.delete(ONLINE_USER_PREFIX + userId);
    }
}
