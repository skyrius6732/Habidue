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
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
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
    private final com.habidue.app.repository.badge.BadgeLevelRuleRepository badgeLevelRuleRepository;
    private final BadgeService badgeService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final NotificationService notificationService;

    // [시니어 조치] 탈퇴 시 데이터 정리를 위한 레포지토리 추가
    private final com.habidue.app.repository.notice.UserNoticeRepository userNoticeRepository;
    private final com.habidue.app.repository.tag.UserTagRepository userTagRepository;
    private final com.habidue.app.repository.message.MessageRepository messageRepository;
    private final com.habidue.app.repository.notification.NotificationRepository notificationRepository;
    private final com.habidue.app.repository.user.AttendanceRepository attendanceRepository;

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

        // [시니어 조치] 양방향 관계 설정 (Cascade 저장 유도)
        user.setActivityStats(UserActivityStats.createEmpty(user));
        
        return userRepository.save(user);
    }

    @Transactional
    public UserActivityResponseDto getUserActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        userActivityStatsRepository.insertIgnore(userId);
        UserActivityStats stats = userActivityStatsRepository.findById(userId).orElseThrow();

        List<com.habidue.app.dto.badge.BadgeResponseDto> badges = badgeService.getMyBadgeDtos(user);
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
            
            try {
                redisTemplate.opsForValue().set(BADGE_RULES_CACHE_KEY, objectMapper.writeValueAsString(badgeRules), 1, java.util.concurrent.TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Badge Rules Cache Write Error: {}", e.getMessage());
            }
        }

        long totalRank = userRepository.calculateRankByTotalExp(user.getTotalExp());
        long totalUserCount = userRepository.count();

        return UserActivityResponseDto.of(stats, badges, badgeRules, totalRank, totalUserCount);
    }

    @Transactional
    public void syncUserActivity(Long userId) {
        UserActivityStats stats = userActivityStatsRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("활동 통계 정보가 없습니다."));
        badgeService.checkAndAwardBadges(stats);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier)) // 기존 로직 호환성을 위해 유지하되 순서 변경
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + identifier));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. ID: " + userId));
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
    public void deleteUser(Long userId) {
        // [시니어 조치] ID를 기반으로 영속성 컨텍스트 내에서 다시 조회하여 LazyInitialization 방지
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("탈퇴를 진행할 사용자를 찾을 수 없습니다."));

        log.info("Starting withdrawal process for user: {} (ID: {})", user.getUsername(), user.getId());

        // 1. CascadeType.ALL 및 orphanRemoval 컬렉션 비우기
        user.getUserNotices().clear();
        user.getUserTags().clear();
        user.getAttendances().clear();

        // 2. 사적 데이터 벌크 삭제 (Modifying 쿼리 사용)
        notificationRepository.deleteByUser(user);
        messageRepository.deleteBySenderOrReceiver(user, user);
        userBadgeRepository.deleteByUserId(user.getId());

        // 3. 통계 데이터 직접 초기화
        userActivityStatsRepository.resetStatsByUserId(user.getId());

        // 4. 유저 익명화 및 상태 변경
        user.withdraw();
        
        // 5. 즉시 DB 반영
        userRepository.saveAndFlush(user);
        
        log.info("User withdrawal completed successfully: {}", userId);
    }

    @Transactional
    public User updateEquippedBadge(Long userId, Long badgeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        if (badgeId != null) {
            boolean hasBadge = userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId);
            if (!hasBadge) {
                throw new IllegalArgumentException("보유하지 않은 배지는 장착할 수 없습니다.");
            }
        }
        
        user.setEquippedBadgeId(badgeId);
        return userRepository.save(user);
    }

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
        
        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);

        if (status == UserStatus.BLOCKED) {
            String finalReason = (reason != null && !reason.trim().isEmpty()) ? reason : "관리자에 의해 계정이 차단되었습니다.";
            user.setBlockedReason(finalReason);
            user.setBlockedAt(LocalDateTime.now());
            redisTemplate.opsForValue().set(BLOCKED_USER_PREFIX + userId, finalReason);
            
            // [시니어 조치] 차단 알림 발송 (기록용)
            notificationService.send(user, NotificationType.SYSTEM, "🚫 운영 정책 위반으로 인해 계정이 차단되었습니다. (사유: " + finalReason + ")", null, null);
        } else {
            user.setBlockedReason(null);
            user.setBlockedAt(null);
            redisTemplate.delete(BLOCKED_USER_PREFIX + userId);

            // [시니어 조치] 복구 알림 발송
            if (oldStatus == UserStatus.BLOCKED && status == UserStatus.ACTIVE) {
                notificationService.send(user, NotificationType.SYSTEM, "📢 관리자에 의해 계정 제한이 해제되었습니다. 다시 정상적인 이용이 가능합니다.", null, null);
            }
        }
        
        return userRepository.save(user);
    }

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
