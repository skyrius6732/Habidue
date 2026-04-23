package com.habidue.app.service.user;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserEffect;
import com.habidue.app.domain.user.UserEffect.EffectSource;
import com.habidue.app.repository.user.UserEffectRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [시니어 조치] 사용자 이펙트 관리 서비스
 * - 베타/이벤트/상점 이펙트 획득 및 소유 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEffectService {

    private final UserEffectRepository userEffectRepository;
    private final UserRepository userRepository;

    /**
     * 사용자에게 이펙트 지급
     */
    @Transactional
    public UserEffect grantEffect(Long userId, String effectCode, EffectSource source) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 중복 지급 방지
        if (userEffectRepository.existsByUserIdAndEffectCode(userId, effectCode)) {
            log.warn("Effect already owned by user. userId={}, effectCode={}", userId, effectCode);
            return userEffectRepository.findByUserIdAndEffectCode(userId, effectCode).orElse(null);
        }

        UserEffect userEffect = UserEffect.builder()
            .user(user)
            .effectCode(effectCode)
            .source(source)
            .build();

        UserEffect saved = userEffectRepository.save(userEffect);
        log.info("Effect granted. userId={}, effectCode={}, source={}", userId, effectCode, source);
        return saved;
    }

    /**
     * 사용자가 특정 이펙트를 소유하는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasEffect(Long userId, String effectCode) {
        return userEffectRepository.hasEffect(userId, effectCode);
    }

    /**
     * 사용자의 모든 소유 이펙트 코드 조회
     */
    @Transactional(readOnly = true)
    public List<String> getUserEffectCodes(Long userId) {
        return userEffectRepository.findByUserId(userId).stream()
            .map(UserEffect::getEffectCode)
            .collect(Collectors.toList());
    }

    /**
     * 사용자의 특정 소스 이펙트 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getUserEffectsBySource(Long userId, EffectSource source) {
        return userEffectRepository.findByUserIdAndSource(userId, source).stream()
            .map(UserEffect::getEffectCode)
            .collect(Collectors.toList());
    }

    /**
     * 베타테스터 여부 확인 및 베타 이펙트 지급
     */
    @Transactional
    public void setBetaTester(Long userId, boolean betaTester) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setBetaTester(betaTester);
        userRepository.save(user);

        // 베타테스터 추가 시 화이트 윙 지급
        if (betaTester && !userEffectRepository.existsByUserIdAndEffectCode(userId, "PIONEER_WINGS")) {
            grantEffect(userId, "PIONEER_WINGS", EffectSource.BETA);
            log.info("Beta tester onboarded. userId={}, PIONEER_WINGS granted", userId);
        }
    }

    /**
     * 특정 이펙트를 사용자에게서 제거
     * 장착 중인 이펙트라면 equippedEffect도 함께 해제
     */
    @Transactional
    public void revokeEffect(Long userId, String effectCode) {
        // 1단계: 회수 대상 이펙트가 장착 중이라면 먼저 장착 해제
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && effectCode.equals(user.getEquippedEffect())) {
            user.setEquippedEffect(null);
            userRepository.save(user);
            log.info("Equipped effect unequipped before revoke. userId={}, effectCode={}", userId, effectCode);
        }

        // 2단계: UserEffect 레코드 삭제
        userEffectRepository.findByUserIdAndEffectCode(userId, effectCode)
            .ifPresent(userEffectRepository::delete);

        log.info("Effect revoked. userId={}, effectCode={}", userId, effectCode);
    }
}
