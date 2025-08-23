package com.group.defectapp.controller.user;

import com.group.defectapp.config.CustomUserDetails;
import com.group.defectapp.config.CustomUserDetailsService;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.service.user.UserService;
import com.group.defectapp.util.CookieUtil;
import com.group.defectapp.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    // 비밀번호 실패 한계값 (예: 5회)
    private static final int PASSWORD_FAIL_LIMIT = 5;
    private static final String PASSWORD_FAIL_LIMIT_MSG = "비밀번호를 5회 이상 틀렸습니다.\n관리자에게 문의하세요.";
    private static final String LOGIN_FAIL_MSG = "아이디 또는 비밀번호가 올바르지 않습니다.";

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody LoginRequestDto loginRequest, HttpServletResponse response) {
        String userId = loginRequest.getUserId();

        try {
            // 1. 사용자 존재 여부 및 계정 잠금 상태 확인
            if (!userDetailsService.existsByUserId(userId)) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
            }

            // 2. 사용자 정보 조회하여 비밀번호 실패 횟수 확인
            var user = userService.findByUserId(userId);
            if (isOverPwdFailLimit(user.getPwdFailCnt())) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("ACCOUNT_LOCKED", PASSWORD_FAIL_LIMIT_MSG));
            }

            // 3. 인증 수행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userId, loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 4. 로그인 성공 처리
            userService.resetPwdFailCnt(userId);  // 실패 카운트 초기화
            userService.updateLastLoginAt(userId);  // 마지막 로그인 시간 업데이트

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 쿠키 설정
            cookieUtil.setJwtCookies(response, accessToken, refreshToken);
            cookieUtil.setUserInfoCookie(response, userDetails);

            // 응답 데이터 구성
            Map<String, Object> body = new HashMap<>();
            body.put("result", "success");
            body.put("userId", userDetails.getUsername());
            body.put("userName", userDetails.getUserName());
            body.put("userSeCd", userDetails.getUserSeCd());
            body.put("authorities", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList()));

            log.info("로그인 성공: {}", userId);
            return ResponseEntity.ok(body);

        } catch (BadCredentialsException e) {
            // 5. 인증 실패 시 실패 카운트 증가
            try {
                int failCount = userService.updatePwdFailCnt(userId);
                String errorMessage = isOverPwdFailLimit(failCount) ? PASSWORD_FAIL_LIMIT_MSG : LOGIN_FAIL_MSG;
                String errorCode = isOverPwdFailLimit(failCount) ? "ACCOUNT_LOCKED" : "LOGIN_FAILED";

                log.warn("로그인 실패 - 비밀번호 오류: {} (실패 횟수: {})", userId, failCount);
                return ResponseEntity.status(401)
                        .body(createErrorResponse(errorCode, errorMessage));
            } catch (Exception ex) {
                log.error("로그인 실패 처리 중 오류", ex);
                return ResponseEntity.status(401)
                        .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
            }
        } catch (UsernameNotFoundException e) {
            log.warn("로그인 실패 - 사용자 없음: {}", userId);
            return ResponseEntity.status(401)
                    .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
        } catch (Exception e) {
            log.error("로그인 처리 중 예상치 못한 오류", e);
            return ResponseEntity.status(401)
                    .body(createErrorResponse("LOGIN_FAILED", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = cookieUtil.getRefreshTokenFromCookies(request);
            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(createErrorResponse("INVALID_TOKEN", "리프레시 토큰이 유효하지 않습니다."));
            }

            String userId = jwtUtil.getUsernameFromToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            String newAccessToken = jwtUtil.generateToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 쿠키 갱신
            cookieUtil.setJwtCookies(response, newAccessToken, newRefreshToken);
            cookieUtil.setUserInfoCookie(response, (CustomUserDetails) userDetails);

            Map<String, Object> body = new HashMap<>();
            body.put("result", "success");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("리프레시 토큰 실패", e);
            return ResponseEntity.status(401).body(createErrorResponse("REFRESH_FAILED", "리프레시 토큰 갱신에 실패하였습니다."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.deleteAuthCookies(response);
        return ResponseEntity.ok(createSuccessResponse("로그아웃되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body(createErrorResponse("UNAUTHORIZED", "인증되지 않았습니다."));
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userDetails.getUsername());
        body.put("userName", userDetails.getUserName());
        body.put("userSeCd", userDetails.getUserSeCd());
        body.put("authorities", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(body);
    }

    @GetMapping("/check-token")
    public ResponseEntity<?> checkToken(HttpServletRequest request) {
        String accessToken = cookieUtil.getAccessTokenFromCookies(request);
        if (accessToken == null || !jwtUtil.validateToken(accessToken)) {
            return ResponseEntity.status(401).body(createErrorResponse("INVALID_TOKEN", "토큰이 유효하지 않습니다."));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("valid", true);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/unlock-account")
    public ResponseEntity<?> unlockAccount(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("INVALID_REQUEST", "사용자 ID가 필요합니다."));
            }

            // 계정 잠금 해제 (실패 카운트 초기화)
            userService.resetPwdFailCnt(userId.trim());
            log.info("계정 잠금 해제: {}", userId);

            return ResponseEntity.ok(createSuccessResponse(userId + " 계정이 잠금 해제되었습니다."));
        } catch (Exception e) {
            log.error("계정 잠금 해제 중 오류", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("UNLOCK_FAILED", "계정 잠금 해제 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgetPassword(@RequestBody Map<String, Object> userInfo) {
        String userId = Objects.toString(userInfo.get("email"));
        String retForgotPassword = userService.forgetPassword(userId);
        return ResponseEntity.ok(retForgotPassword);
    }

    /**
     * 비밀번호 실패 횟수가 제한을 초과했는지 확인
     */
    private boolean isOverPwdFailLimit(int failCount) {
        return failCount >= PASSWORD_FAIL_LIMIT;
    }

    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", error);
        map.put("message", message);
        return map;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }
}