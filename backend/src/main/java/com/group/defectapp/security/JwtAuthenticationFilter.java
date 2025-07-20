package com.group.defectapp.config;

import com.group.defectapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
        log.info("Authorization Header: {}", authHeader != null ? "Bearer ***" : "없음");

        // 인증이 필요없는 경로 체크
        if (isPublicPath(requestURI)) {
            log.info("공개 경로 - 인증 건너뜀: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더가 없거나 Bearer로 시작하지 않는 경우
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(jwtConfig.getPrefix())) {
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아닙니다 - URI: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 토큰 추출
            final String token = jwtUtil.getTokenFromHeader(authHeader);
            log.info("토큰 추출 완료: {}...", token != null ? token.substring(0, Math.min(token.length(), 20)) : "null");

            if (!StringUtils.hasText(token)) {
                log.warn("토큰이 비어있습니다 - URI: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰에서 사용자명 추출
            final String username = jwtUtil.getUsernameFromToken(token);
            log.info("토큰에서 사용자명 추출: {}", username);

            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 사용자 정보 로드
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("사용자 정보 로드 완료: {}, 권한: {}", userDetails.getUsername(), userDetails.getAuthorities());

                // 토큰 유효성 검사
                if (jwtUtil.validateToken(token, userDetails)) {
                    log.info("토큰 유효성 검사 통과");

                    // 인증 객체 생성 및 설정
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("인증 정보 설정 완료 - 사용자: {}, 권한: {}",
                            userDetails.getUsername(), userDetails.getAuthorities());
                } else {
                    log.error("토큰 유효성 검사 실패 - 사용자: {}", username);
                }
            } else {
                if (!StringUtils.hasText(username)) {
                    log.error("토큰에서 사용자명을 추출할 수 없습니다.");
                }
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    log.info("이미 인증된 사용자입니다.");
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 중 오류 발생 - URI: {}, 오류: {}", requestURI, e.getMessage(), e);
            // 인증 실패 시 SecurityContext 초기화
            SecurityContextHolder.clearContext();
        }

        log.info("JWT 필터 처리 완료 - 인증 상태: {}",
                SecurityContextHolder.getContext().getAuthentication() != null ? "인증됨" : "인증안됨");
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/public/") ||
                path.startsWith("/h2-console/") ||
                path.startsWith("/error") ||
                path.startsWith("/favicon.ico") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/uploads/") ||

                // 로그인, 회원가입, 토큰 갱신 등 인증 불필요 경로 추가
                path.equals("/auth/sign-in") ||
                path.equals("/auth/signup") ||
                path.equals("/auth/refresh");
    }

}