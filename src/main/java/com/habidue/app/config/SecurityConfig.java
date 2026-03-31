package com.habidue.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final com.habidue.app.service.user.CustomOAuth2UserService customOAuth2UserService;
    private final com.habidue.app.config.oauth.OAuth2SuccessHandler oAuth2SuccessHandler;
    private final com.habidue.app.config.oauth.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final com.habidue.app.config.jwt.JwtTokenProvider jwtTokenProvider;
    private final com.habidue.app.config.jwt.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final StringRedisTemplate redisTemplate;
    private final com.habidue.app.service.user.UserService userService;

    public SecurityConfig(com.habidue.app.service.user.CustomOAuth2UserService customOAuth2UserService,
                          com.habidue.app.config.oauth.OAuth2SuccessHandler oAuth2SuccessHandler,
                          com.habidue.app.config.oauth.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                          com.habidue.app.config.jwt.JwtTokenProvider jwtTokenProvider,
                          com.habidue.app.config.jwt.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          StringRedisTemplate redisTemplate,
                          com.habidue.app.service.user.UserService userService) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/uploads/**", "/favicon.ico", "/error");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**", "/api/auth/reissue", "/error", "/favicon.ico", "/uploads/**", "/api/ranking/**", "/api/notifications/subscribe", "/api/notifications/token", "/api/announce-patch/**", "/api/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/notices/**", "/api/user-notices/**", "/api/keywords/**", "/api/user-tags/**", "/api/attendance/**", "/api/users/**", "/api/notifications/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .addFilterBefore(new com.habidue.app.config.jwt.JwtAuthenticationFilter(jwtTokenProvider, jwtAuthenticationEntryPoint, redisTemplate, userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // [시니어 조치] 로컬, 운영, 모바일 도메인 모두 수용 가능하도록 설정 확장
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:5173",
            "https://habidue.com",
            "https://www.habidue.com",
            "http://54.180.67.106:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Authorization-Refresh"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Authorization-Refresh"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
