package com.habidue.app.service.user;

import com.habidue.app.config.oauth.GoogleUserInfo;
import com.habidue.app.config.oauth.KakaoUserInfo;
import com.habidue.app.config.oauth.NaverUserInfo;
import com.habidue.app.config.oauth.OAuth2UserInfo;
import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.user.UserStatus;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final UserService userService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        OAuth2UserInfo oAuth2UserInfo = null;
        if (registrationId.equals("google")) {
            oAuth2UserInfo = new GoogleUserInfo(attributes);
        } else if (registrationId.equals("naver")) {
            oAuth2UserInfo = new NaverUserInfo(attributes);
        } else if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoUserInfo(attributes);
        } else {
            log.error("지원하지 않는 소셜 로그인 제공자: {}", registrationId);
            throw new OAuth2AuthenticationException("unsupported_provider");
        }

        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();

        log.info("[OAuth2 Login] Provider: {}, ProviderId: {}, Email: {}", provider, providerId, email);

        // 1. Provider 정보로 조회
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(provider, providerId);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            
            // [추가] 차단된 사용자인지 확인
            if (existingUser.getStatus() == UserStatus.BLOCKED) {
                String reason = existingUser.getBlockedReason() != null ? existingUser.getBlockedReason() : "관리자에 의해 계정이 차단되었습니다.";
                throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("user_blocked", reason, null));
            }

            // [시니어 조치] 탈퇴 유저 처리
            if (existingUser.getStatus() == UserStatus.WITHDRAWN) {
                java.time.LocalDateTime withdrawalAt = existingUser.getWithdrawalAt();
                if (withdrawalAt != null && java.time.LocalDateTime.now().isBefore(withdrawalAt.plusDays(7))) {
                    // 7일 이내: 차단
                    java.time.LocalDateTime canReregisterAt = withdrawalAt.plusDays(7);
                    String reason = "7일 이내 탈퇴한 계정입니다. " + 
                        canReregisterAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "부터 재가입이 가능합니다.";
                    throw new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("user_withdrawn", reason, null));
                } else {
                    // 7일 경과: 기존 유저 완전 익명화(Unique 해제) 후 신규 가입 프로세스로 전환
                    userService.forceAnonymizeUser(existingUser);
                    userOptional = Optional.empty(); 
                }
            }
        }

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // 기존 계정 정보 업데이트
            if (user.getProvider() == null || !user.getProvider().equals(provider)) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                user = userRepository.save(user);
            }
        } else {
            // [핵심] 신규 유저 생성 (새로운 user_id 부여)
            String suffix = (providerId != null && providerId.length() >= 5) ? providerId.substring(0, 5) : java.util.UUID.randomUUID().toString().substring(0, 5);
            String generatedUsername = name + "_" + suffix;
            
            String finalNickname = name != null ? name : generatedUsername;
            if (userRepository.existsByNickname(finalNickname)) {
                finalNickname = finalNickname + "_" + suffix;
            }

            user = new User();
            user.setUsername(generatedUsername);
            user.setNickname(finalNickname); 
            user.setEmail(email);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setPassword(java.util.UUID.randomUUID().toString());
            
            user.setActivityStats(UserActivityStats.createEmpty(user));
            user = userRepository.save(user);
            log.info("[OAuth2] 신규 사용자 가입 완료 - ID: {}, Username: {}", user.getId(), user.getUsername());
        }

        return UserPrincipal.create(user, attributes);
    }
}
