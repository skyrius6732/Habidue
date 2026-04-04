package com.habidue.app.config.oauth;

import com.habidue.app.config.jwt.JwtTokenProvider;
import com.habidue.app.config.oauth.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String username = userPrincipal.getUsername(); // email -> username 변경

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // Redis에 Refresh Token 저장 (Key: username, Value: refreshToken)
        redisTemplate.opsForValue().set(
                username,
                refreshToken,
                refreshTokenValidityInSeconds,
                TimeUnit.SECONDS
        );

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken);

        // [시니어 조치] 7일 이내 탈퇴한 사용자 로그인 시 안내 메시지 표시
        com.habidue.app.domain.user.User user = userPrincipal.getUser();
        if (user.getStatus() == com.habidue.app.domain.user.UserStatus.WITHDRAWN &&
            user.getWithdrawalAt() != null) {
            java.time.LocalDateTime canReregisterAt = user.getWithdrawalAt().plusDays(7);
            if (java.time.LocalDateTime.now().isBefore(canReregisterAt)) {
                String message = "7일 이내 탈퇴한 계정입니다. " +
                    canReregisterAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                    "부터 재가입이 가능합니다.";
                urlBuilder.queryParam("withdrawalWarning", message);
            }
        }

        String targetUrl = urlBuilder.build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
