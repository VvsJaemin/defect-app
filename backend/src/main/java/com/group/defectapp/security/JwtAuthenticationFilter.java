package com.group.defectapp.config;

import com.group.defectapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();
        final String authHeader = request.getHeader(jwtConfig.getHeader());

        log.info("JWT 필터 처리 시작 - {} {}", method, requestURI);

        // 인증이 필요없는 경로 체크 - 이게 먼저 와야 함!
        if (isPublicPath(requestURI)) {
            log.info("공개 경로 - 인증 건너뜀: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 추출 시도 (헤더 우선, 쿠키 보조)
        String token = null;

        // 1. Authorization 헤더에서 토큰 추출 시도
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(jwtConfig.getPrefix())) {
            token = jwtUtil.getTokenFromHeader(authHeader);
            log.info("헤더에서 토큰 추출 완료");
        }

        // 2. 헤더에 없으면 쿠키에서 추출 시도
        if (!StringUtils.hasText(token)) {
            // CookieUtil 의존성 주입 필요
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        log.info("쿠키에서 토큰 추출 완료");
                        break;
                    }
                }
            }
        }

        // 토큰이 없으면 다음 필터로
        if (!StringUtils.hasText(token)) {
            log.warn("토큰이 없습니다 - URI: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 나머지 인증 로직은 동일...
        try {
            final String username = jwtUtil.getUsernameFromToken(token);
            log.info("토큰에서 사용자명 추출: {}", username);

            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("사용자 정보 로드 완료: {}, 권한: {}", userDetails.getUsername(), userDetails.getAuthorities());

                if (jwtUtil.validateToken(token, userDetails)) {
                    log.info("토큰 유효성 검사 통과");

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("인증 정보 설정 완료 - 사용자: {}, 권한: {}",
                            userDetails.getUsername(), userDetails.getAuthorities());
                } else {
                    log.error("토큰 유효성 검사 실패 - 사용자: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 중 오류 발생 - URI: {}, 오류: {}", requestURI, e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        log.info("JWT 필터 처리 완료 - 인증 상태: {}",
                SecurityContextHolder.getContext().getAuthentication() != null ? "인증됨" : "인증안됨");
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/public/") ||
                path.startsWith("/auth/") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/error") ||
                path.startsWith("/favicon.ico") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/uploads/");
    }

}