package com.habidue.app.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 추가
import com.habidue.app.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()); // JavaTimeModule 등록

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.error("Responding with unauthorized error. Message - {}", authException.getMessage());
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String errorMessage = authException.getMessage();
        if (authException.getCause() instanceof io.jsonwebtoken.ExpiredJwtException) {
            errorMessage = "토큰이 만료되었습니다.";
        } else if (authException.getCause() instanceof io.jsonwebtoken.MalformedJwtException || authException.getCause() instanceof io.jsonwebtoken.security.SignatureException) {
            errorMessage = "유효하지 않은 토큰입니다.";
        } else if (errorMessage == null) {
            errorMessage = "인증에 실패했습니다.";
        }

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(authException.getClass().getSimpleName())
                .message(errorMessage)
                .data(null)
                .build();
        
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
