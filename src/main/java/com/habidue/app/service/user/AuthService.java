package com.habidue.app.service.user;

import com.habidue.app.config.jwt.JwtTokenProvider;
import com.habidue.app.repository.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException; // 추가
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    public String reissueAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검사 (만료 여부 포함)
        try {
            if (!StringUtils.hasText(refreshToken) || !jwtTokenProvider.validateToken(refreshToken)) {
                throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
            }
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh Token이 만료되었습니다.", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Refresh Token 검증 중 오류 발생.", e);
        }

        // 2. Refresh Token에서 사용자 이메일 추출
        Claims claims = Jwts.parserBuilder().setSigningKey(jwtTokenProvider.getKey()).build().parseClaimsJws(refreshToken).getBody();
        String email = claims.getSubject();
        
        // 3. Redis에 저장된 토큰과 일치하는지 확인
        String storedRefreshToken = redisTemplate.opsForValue().get(email);
        if (storedRefreshToken == null) {
            throw new IllegalArgumentException("Redis에 Refresh Token이 존재하지 않습니다. 다시 로그인해주세요.");
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다. 다시 로그인해주세요.");
        }

        // 4. DB에서 사용자 정보를 다시 조회하여 Authentication 객체 생성
        Authentication authentication = userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        com.habidue.app.config.oauth.UserPrincipal.create(user),
                        null,
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().getKey()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다: " + email));

        // 5. 새로운 Access Token 생성
        return jwtTokenProvider.createAccessToken(authentication);
    }
}
