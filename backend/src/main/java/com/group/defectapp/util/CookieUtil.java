package com.group.defectapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.defectapp.config.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CookieUtil {

    private final ObjectMapper objectMapper;

    @Value("${jwt.expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @Value("${cookie.domain}")
    private String cookieDomain;

    public CookieUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 쿠키 생성 및 설정 (기본 설정)
     */
    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly) {
        setCookie(response, name, value, maxAgeInSeconds, httpOnly, cookieSecure);
    }

    /**
     * 쿠키 생성 및 설정 (상세 설정)
     */
    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeInSeconds);

        // 도메인 설정 (localhost가 아닌 경우만)
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);

        // SameSite 설정 (쿠키 헤더에 직접 추가)
        setSameSiteAttribute(response, name, cookieSameSite);

        log.debug("쿠키 설정 완료: {} (secure: {}, httpOnly: {}, maxAge: {}초)",
                name, secure, httpOnly, maxAgeInSeconds);
    }

    /**
     * JWT 토큰 쿠키 설정 (Access Token과 Refresh Token)
     */
    public void setJwtCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
    }

    /**
     * Access Token 쿠키 설정 (HttpOnly와 클라이언트 접근 가능한 두 가지 버전)
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        int accessTokenMaxAge = (int) (accessTokenExpiration / 1000); // 밀리초를 초로 변환

        // HttpOnly 쿠키 (보안용) - 서버에서만 접근 가능
        setCookie(response, "accessToken", accessToken, accessTokenMaxAge, true, cookieSecure);

        // 클라이언트에서 읽을 수 있는 쿠키 (UI 상태 관리용) - JavaScript에서 접근 가능
        setCookie(response, "clientAccessToken", accessToken, accessTokenMaxAge, false, cookieSecure);
    }

    /**
     * Refresh Token 쿠키 설정
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int refreshTokenMaxAge = (int) (refreshTokenExpiration / 1000); // 밀리초를 초로 변환
        setCookie(response, "refreshToken", refreshToken, refreshTokenMaxAge, true, cookieSecure);
    }

    /**
     * 사용자 정보 쿠키 설정 (JSON 형태로 URL 인코딩)
     */
    public void setUserInfoCookie(HttpServletResponse response, CustomUserDetails userDetails) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userDetails.getUsername());
        userInfo.put("userName", userDetails.getUserName());
        userInfo.put("userSeCd", userDetails.getUserSeCd());
        userInfo.put("authorities", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));

        try {
            String userInfoJson = objectMapper.writeValueAsString(userInfo);
            String userInfoEncoded = URLEncoder.encode(userInfoJson, StandardCharsets.UTF_8);

            int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);
            setCookie(response, "userInfo", userInfoEncoded, accessTokenMaxAge, false, cookieSecure);
        } catch (Exception e) {
            log.error("userInfo 쿠키 직렬화 실패: {}", e.getMessage());
        }
    }

    /**
     * 요청에서 특정 이름의 쿠키 값 가져오기
     */
    public String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Access Token 쿠키에서 값 가져오기
     */
    public String getAccessTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, "accessToken");
    }

    /**
     * Refresh Token 쿠키에서 값 가져오기
     */
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, "refreshToken");
    }

    /**
     * 쿠키 삭제
     */
    public void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setSecure(cookieSecure);
        cookie.setHttpOnly(isHttpOnlyCookie(cookieName));

        // 도메인 설정 (localhost가 아닌 경우만)
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);

        log.debug("쿠키 삭제 완료: {}", cookieName);
    }

    /**
     * 여러 쿠키 동시 삭제
     */
    public void deleteCookies(HttpServletResponse response, String... cookieNames) {
        for (String cookieName : cookieNames) {
            deleteCookie(response, cookieName);
        }
    }

    /**
     * 인증 관련 모든 쿠키 삭제
     */
    public void deleteAuthCookies(HttpServletResponse response) {
        deleteCookies(response, "accessToken", "refreshToken", "userInfo", "clientAccessToken");
    }

    /**
     * 쿠키가 HttpOnly 여부 확인
     */
    private boolean isHttpOnlyCookie(String cookieName) {
        return "accessToken".equals(cookieName) || "refreshToken".equals(cookieName);
    }

    /**
     * 쿠키 존재 여부 확인
     */
    public boolean hasCookie(HttpServletRequest request, String cookieName) {
        return getTokenFromCookies(request, cookieName) != null;
    }

    /**
     * SameSite 속성 설정 (쿠키 헤더에 직접 추가)
     */
    private void setSameSiteAttribute(HttpServletResponse response, String cookieName, String sameSite) {
        if (sameSite != null && !sameSite.isEmpty()) {
            String headerValue = String.format("%s=; SameSite=%s", cookieName, sameSite);
            response.addHeader("Set-Cookie", headerValue);
        }
    }

    /**
     * 현재 환경 정보 로깅
     */
    public void logEnvironmentInfo() {
        log.info("=== 쿠키 환경 설정 정보 ===");
        log.info("Secure: {}", cookieSecure);
        log.info("SameSite: {}", cookieSameSite);
        log.info("Domain: {}", cookieDomain);
        log.info("Access Token 만료시간: {}초", accessTokenExpiration / 1000);
        log.info("Refresh Token 만료시간: {}초", refreshTokenExpiration / 1000);
        log.info("========================");
    }

    /**
     * 모든 쿠키 정보 로깅 (디버깅용)
     */
    public void logAllCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            log.debug("요청에 쿠키가 없습니다.");
            return;
        }

        log.debug("=== 현재 요청의 쿠키 정보 ===");
        for (Cookie cookie : request.getCookies()) {
            log.debug("Cookie: {} = {} (secure: {}, httpOnly: {})",
                    cookie.getName(), cookie.getValue(), cookie.getSecure(), cookie.isHttpOnly());
        }
        log.debug("============================");
    }
}