package com.group.defectapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.defectapp.config.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
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

    @Value("${cookie.domain:}")
    private String cookieDomain;

    public CookieUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly) {
        setCookie(response, name, value, maxAgeInSeconds, httpOnly, cookieSecure);
    }

    public void setCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds, boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeInSeconds);

        if (cookieDomain != null && !cookieDomain.isBlank() && !"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
        setSameSiteAttribute(response, name, cookieSameSite);
    }

    private void setSameSiteAttribute(HttpServletResponse response, String name, String sameSite) {
        // SameSite 속성 설정 (Spring Boot 3 미만 환경 대응)
    }

    public void setJwtCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
        setTokenExpiryCookie(response);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);
        setCookie(response, "accessToken", accessToken, accessTokenMaxAge, false, cookieSecure);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int refreshTokenMaxAge = (int) (refreshTokenExpiration / 1000);
        setCookie(response, "refreshToken", refreshToken, refreshTokenMaxAge, true, cookieSecure);
    }

    public void setTokenExpiryCookie(HttpServletResponse response) {
        long expiryTime = System.currentTimeMillis() + accessTokenExpiration;
        int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);

        setCookie(response, "tokenExpiry", String.valueOf(expiryTime), accessTokenMaxAge, false, cookieSecure);
    }

    public void setUserInfoCookie(HttpServletResponse response, CustomUserDetails userDetails) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userDetails.getUsername());
        userInfo.put("userName", userDetails.getUserName());
        userInfo.put("userSeCd", userDetails.getUserSeCd());
        userInfo.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        try {
            String userInfoJson = objectMapper.writeValueAsString(userInfo);
            String userInfoEncoded = URLEncoder.encode(userInfoJson, StandardCharsets.UTF_8);
            int accessTokenMaxAge = (int) (accessTokenExpiration / 1000);

            setCookie(response, "userInfo", userInfoEncoded, accessTokenMaxAge, false, cookieSecure);

        } catch (Exception e) {
            log.error("사용자 정보 쿠키 설정 실패", e);
        }
    }

    public void deleteAuthCookies(HttpServletResponse response) {
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
        deleteCookie(response, "tokenExpiry");
        deleteCookie(response, "userInfo");
    }

    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(false);
        cookie.setSecure(cookieSecure);

        if (cookieDomain != null && !cookieDomain.isBlank() && !"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
        setSameSiteAttribute(response, name, cookieSameSite);
    }

    public String getAccessTokenFromCookies(jakarta.servlet.http.HttpServletRequest request) {
        return getCookieValue(request, "accessToken");
    }

    public String getRefreshTokenFromCookies(jakarta.servlet.http.HttpServletRequest request) {
        return getCookieValue(request, "refreshToken");
    }

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