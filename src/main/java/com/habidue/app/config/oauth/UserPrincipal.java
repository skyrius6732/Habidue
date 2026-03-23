package com.habidue.app.config.oauth;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class UserPrincipal implements OAuth2User, UserDetails {
    private Long id;
    private String email;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;
    private User user; // [시니어 조치] 엔티티 보유 (로그인 직후엔 존재, JWT 인증 시엔 null일 수 있음)

    // 기존 생성자 복구 (JwtTokenProvider 등에서 사용)
    public UserPrincipal(Long id, String email, UserStatus status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.authorities = authorities;
    }

    // 엔티티 기반 생성자 (OAuth2 등에서 사용)
    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.status = user.getStatus();
        this.authorities = authorities;
        this.user = user;
    }

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getKey()));
        return new UserPrincipal(user, authorities);
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }
    
    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status != UserStatus.BLOCKED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == UserStatus.ACTIVE; }
    
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    @Override
    public String getName() { return String.valueOf(id); }
}
