
package com.group.defectapp.security;

import com.group.defectapp.config.JwtConfig;
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
        final String authHeader = request.getHeader(jwtConfig.getHeader());

        // 공개 경로는 인증 없이 통과
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 추출 (헤더 우선, 쿠키 보조)
        String token = extractToken(authHeader, request);

        // 토큰이 없으면 다음 필터로
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        authenticateToken(token, request);
        filterChain.doFilter(request, response);
    }

    private String extractToken(String authHeader, HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(jwtConfig.getPrefix())) {
            return jwtUtil.getTokenFromHeader(authHeader);
        }

        // 2. 쿠키에서 토큰 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void authenticateToken(String token, HttpServletRequest request) {
        try {
            final String username = jwtUtil.getUsernameFromToken(token);

            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    setAuthentication(userDetails, request);
                }
            }
        } catch (Exception e) {
            log.warn("JWT 인증 실패 - URI: {}, 오류: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/public/") ||
                path.startsWith("/auth/") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/error") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/uploads/") ||
                // Actuator health 엔드포인트 추가
                path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                // OpenAPI/SpringDoc 관련 경로
                path.startsWith("/api-docs") ||
                path.equals("/api-docs.yaml") ||
                path.startsWith("/swagger-ui/") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/redoc") ||
                path.equals("/redoc.html");
    }
}