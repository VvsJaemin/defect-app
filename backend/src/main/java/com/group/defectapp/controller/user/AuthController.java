package com.group.defectapp.controller.user;

import com.group.defectapp.config.CustomUserDetails;
import com.group.defectapp.config.CustomUserDetailsService;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.util.CookieUtil;
import com.group.defectapp.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody LoginRequestDto loginRequest, HttpServletResponse response) {
        try {
            // 인증 수행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 쿠키 설정
            cookieUtil.setJwtCookies(response, accessToken, refreshToken);
            cookieUtil.setUserInfoCookie(response, userDetails);

            // 필요 시 최소 정보 body로도 반환
            Map<String, Object> body = new HashMap<>();
            body.put("result", "success");
            body.put("userId", userDetails.getUsername());
            body.put("userName", userDetails.getUserName());
            body.put("userSeCd", userDetails.getUserSeCd());
            body.put("authorities", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("로그인 실패", e);
            return ResponseEntity.status(401).body(createErrorResponse("LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));
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
        // 실제 구현은 관리자의 액션 및 서비스로 연결
        String userId = request.get("userId");
        // unlock 로직 작성
        return ResponseEntity.ok(createSuccessResponse(userId + " 계정이 잠금 해제되었습니다."));
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