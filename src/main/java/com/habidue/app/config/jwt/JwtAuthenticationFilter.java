package com.habidue.app.config.jwt;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.service.user.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String token = resolveToken(httpServletRequest);
        String requestURI = httpServletRequest.getRequestURI();

        try {
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                
                // [Redis] 실시간 차단 여부 체크
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                String blockedReason = redisTemplate.opsForValue().get(UserService.BLOCKED_USER_PREFIX + principal.getId());
                if (blockedReason != null) {
                    logger.warn("차단된 유저의 접근 시도: {}, uri: {}", principal.getEmail(), requestURI);
                    jwtAuthenticationEntryPoint.commence(httpServletRequest, httpServletResponse, 
                            new BadCredentialsException("USER_BLOCKED:" + blockedReason));
                    return;
                }

                // [Redis] 실시간 접속 상태 갱신 (Heartbeat)
                userService.updateUserOnlineStatus(principal.getId());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            // [시니어 조치] 토큰이 만료되었더라도 즉시 리턴하지 않고 chain.doFilter를 호출함.
            // 그래야 permitAll() 경로(로그인 등)에 대한 요청이 정상적으로 처리됨.
            logger.info("만료된 JWT 토큰입니다. uri: {}", requestURI);
        } catch (Exception e) {
            logger.info("유효하지 않은 JWT 토큰입니다. uri: {}, message: {}", requestURI, e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더(Authorization: Bearer <token>)에서 추출 시도
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. 쿼리 파라미터(?token=<token>)에서 추출 시도 (SSE 및 특정 우회 요청용)
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        
        return null;
    }
}
