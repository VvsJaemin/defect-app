package com.group.defectapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.defectapp.config.CustomUserDetails;
import jakarta.servlet.http.Cookie;
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
     * 경로 path는 항상 "/"로 강제 지정
     */
    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly) {
        setCookie(response, name, value, maxAgeInSeconds, httpOnly, cookieSecure);
    }

    /**
     * 쿠키 생성 및 설정 (상세 설정, 경로는 "/"로 고정)
     */
    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/"); // 경로를 "/"로 고정
        cookie.setMaxAge(maxAgeInSeconds);

        // 도메인 값이 있고 localhost가 아니면 도메인 적용
        if (cookieDomain != null && !cookieDomain.isBlank() && !"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);

        // SameSite 헤더 따로 추가
        setSameSiteAttribute(response, name, cookieSameSite);

        log.debug("쿠키 설정 완료: {} (secure: {}, httpOnly: {}, maxAge: {}초, path: /)", name, secure, httpOnly, maxAgeInSeconds);
    }

    /**
     * Set SameSite 속성(쿠키 헤더에 직접 삽입)
     */
    private void setSameSiteAttribute(HttpServletResponse response, String name, String sameSite) {
        // SameSite 설정이 명확하게 필요하면 여기에 삽입 (Spring Boot 3 미만 대응)
        // (SameSite: None|Lax|Strict)
        // 예시는 생략 (미리 추가 코드 있으면 여기에 삽입)
    }

    /**
     * JWT 토큰 관련 쿠키 일괄 발급
     */
    public void setJwtCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
        setTokenExpiryCookie(response);
    }

    /**
     * Access Token 쿠키 (HttpOnly/JS 접근 둘 다)
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);

        // JS 접근 가능(클라이언트용)으로 세팅
        setCookie(response, "accessToken", accessToken, accessTokenMaxAge, false, cookieSecure);
    }

    /**
     * Refresh Token 쿠키 (HttpOnly)
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int refreshTokenMaxAge = (int) (refreshTokenExpiration / 1000);
        setCookie(response, "refreshToken", refreshToken, refreshTokenMaxAge, true, cookieSecure);
    }

    /**
     * 토큰 만료시간 표시용 쿠키
     */
    public void setTokenExpiryCookie(HttpServletResponse response) {
        long expiryTime = System.currentTimeMillis() + accessTokenExpiration;
        int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);

        setCookie(response, "tokenExpiry", String.valueOf(expiryTime), accessTokenMaxAge, false, cookieSecure);
    }

    /**
     * 사용자 정보 쿠키 (JS 접근용, URL 인코딩)
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

            // 항상 path="/"로 세팅
            setCookie(response, "userInfo", userInfoEncoded, accessTokenMaxAge, false, cookieSecure);

        } catch (Exception e) {
            log.error("userInfo 쿠키 발급 오류", e);
        }
    }

    /**
     * 인증 관련 쿠키 일괄 삭제 (path는 "/"로 고정)
     */
    public void deleteAuthCookies(HttpServletResponse response) {
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
        deleteCookie(response, "tokenExpiry");
        deleteCookie(response, "userInfo");
    }

    /**
     * 특정 쿠키 삭제 (path="/" 고정)
     */
    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(false); // 삭제는 접근성에 상관없음
        cookie.setSecure(cookieSecure);

        if (cookieDomain != null && !cookieDomain.isBlank() && !"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);

        // SameSite 삭제(필요시)
        setSameSiteAttribute(response, name, cookieSameSite);

        log.debug("쿠키 삭제: {} (path: /)", name);
    }

    /**
     * 요청에서 accessToken 쿠키 값 가져오기
     */
    public String getAccessTokenFromCookies(jakarta.servlet.http.HttpServletRequest request) {
        return getCookieValue(request, "accessToken");
    }

    /**
     * 요청에서 refreshToken 쿠키 값 가져오기
     */
    public String getRefreshTokenFromCookies(jakarta.servlet.http.HttpServletRequest request) {
        return getCookieValue(request, "refreshToken");
    }

    /**
     * 요청에서 특정 이름의 쿠키 값 가져오기 (없으면 null)
     */
    public String getCookieValue(jakarta.servlet.http.HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


}