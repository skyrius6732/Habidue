package com.habidue.app.config.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 차단된 경우 등의 인증 실패 시 리다이렉트할 경로 (프론트엔드 주소)
        String targetUrl = "http://localhost:5173/blocked"; 
        
        // [시니어 조치] OAuth2 에러 객체에서 상세 사유(Description)를 우선적으로 추출
        String reason = "관리자의 판단에 의해 계정이 차단되었습니다.";
        if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
            String description = ((org.springframework.security.oauth2.core.OAuth2AuthenticationException) exception).getError().getDescription();
            if (description != null && !description.trim().isEmpty()) {
                reason = description;
            }
        } else {
            // 다른 종류의 인증 예외인 경우 메시지 확인
            String msg = exception.getMessage();
            if (msg != null && !msg.trim().isEmpty()) {
                reason = msg;
            }
        }
        
        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("reason", reason)
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
