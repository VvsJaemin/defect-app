package com.group.defectapp.config;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("사용자 로드: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        return new CustomUserDetails(user);
    }

    // 로그인 성공 시 호출
    @Transactional
    public void updateLastLoginAt(String userId) {
        try {
            log.info("로그인 성공 처리: {}", userId);
            userRepository.updateLastLoginAt(userId, LocalDateTime.now());
            userRepository.resetPwdFailCnt(userId);
        } catch (Exception e) {
            log.error("로그인 성공 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("로그인 성공 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 로그인 실패 시 호출
    @Transactional
    public void updateLoginFailure(String userId) {
        try {
            log.info("로그인 실패 처리: {}", userId);
            userRepository.updatePwnFailedCnt(userId);
        } catch (Exception e) {
            log.error("로그인 실패 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("로그인 실패 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 계정 잠금 해제
    @Transactional
    public void unlockAccount(String userId) {
        try {
            log.info("계정 잠금 해제: {}", userId);
            userRepository.unlockAccount(userId);
        } catch (Exception e) {
            log.error("계정 잠금 해제 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("계정 잠금 해제 중 오류가 발생했습니다.", e);
        }
    }

    // 사용자 존재 여부 확인
    public boolean existsByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }
}