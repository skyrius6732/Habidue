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

        // 1. Provider 정보로만 조회 (이메일 기반 자동 병합 제거)
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();

            // [추가] 차단된 사용자인지 확인
            if (user.getStatus() == UserStatus.BLOCKED) {
                String reason = user.getBlockedReason();
                if (reason == null || reason.trim().isEmpty()) {
                    reason = "관리자의 판단에 의해 계정이 차단되었습니다.";
                }
                // OAuth2Error를 사용하여 사유(Description)를 명확히 전달
                org.springframework.security.oauth2.core.OAuth2Error oauth2Error = 
                    new org.springframework.security.oauth2.core.OAuth2Error("user_blocked", reason, null);
                throw new OAuth2AuthenticationException(oauth2Error);
            }

            // 기존 계정에 소셜 정보가 일치하는지 확인 (이미 위에서 조회했으므로 업데이트 로직 간소화)
            if (user.getProvider() == null || !user.getProvider().equals(provider)) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                user = userRepository.save(user);
            }
        } else {
            // 신규 유저 생성
            String suffix = (providerId != null && providerId.length() >= 5) ? providerId.substring(0, 5) : java.util.UUID.randomUUID().toString().substring(0, 5);
            String generatedUsername = name + "_" + suffix;
            
            // 닉네임 중복 방지 로직 도입
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
            user.setPassword(java.util.UUID.randomUUID().toString());
            
            // [시니어 조치] 양방향 관계 설정 (User 저장 시 Stats도 Cascade로 함께 생성됨)
            user.setActivityStats(UserActivityStats.createEmpty(user));
            user = userRepository.save(user);
        }

        return UserPrincipal.create(user, attributes);
    }
}
