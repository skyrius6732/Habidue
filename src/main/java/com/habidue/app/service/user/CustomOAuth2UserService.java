package com.habidue.app.service.user;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.user.Role;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.user.UserStatus;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

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
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // 1. Provider 정보로 먼저 조회
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(provider, providerId);
        
        // 2. 없으면 이메일로 다시 조회 (init_data.sql 등으로 생성된 기존 계정 체크)
        if (userOptional.isEmpty() && email != null) {
            userOptional = userRepository.findByEmail(email);
        }

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

            // 기존 계정에 소셜 정보가 없다면 업데이트 (연동)
            if (user.getProvider() == null || !user.getProvider().equals(provider)) {
                user.setProvider(provider);
                user.setProviderId(providerId);
                user = userRepository.save(user);
            }
        } else {
            // 신규 유저 생성
            String generatedUsername = name + "_" + (providerId != null ? providerId.substring(0, 5) : java.util.UUID.randomUUID().toString().substring(0, 5));
            user = new User();
            user.setUsername(generatedUsername);
            user.setNickname(name != null ? name : generatedUsername); // 소셜 이름이 있으면 닉네임으로 사용
            user.setEmail(email);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setRole(Role.USER);
            user.setPassword(java.util.UUID.randomUUID().toString());
            user = userRepository.save(user);

            // [시니어 조치] 소셜 로그인 신규 유저 활동 통계 엔티티 생성
            userActivityStatsRepository.save(UserActivityStats.createEmpty(user));
        }

        return UserPrincipal.create(user, attributes);
    }
}
