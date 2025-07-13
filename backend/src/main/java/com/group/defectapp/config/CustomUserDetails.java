package com.group.defectapp.config;

import com.group.defectapp.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 사용자 권한 설정 (user_se_cd 기반)
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserSeCd()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 5회 이상 로그인 실패 시 계정 잠금
        return user.getPwdFailCnt() < 5;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 추가: 사용자 정보 접근을 위한 메서드
    public String getUserName() {
        return user.getUserName();
    }

    public String getUserSeCd() {
        return user.getUserSeCd();
    }

    public LocalDateTime getLastLoginAt() {
        return user.getLastLoginAt();
    }

    public LocalDateTime getFirstRegDtm() {
        return user.getCreatedAt();
    }

    public Integer getPwdFailCnt() {
        return user.getPwdFailCnt();
    }
}