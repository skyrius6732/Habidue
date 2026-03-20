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
        
        String errorMessage = exception.getMessage();
        
        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("reason", errorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
