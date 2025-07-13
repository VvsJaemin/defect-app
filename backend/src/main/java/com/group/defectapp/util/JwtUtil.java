package com.group.defectapp.util;

import com.group.defectapp.config.CustomUserDetails;
import com.group.defectapp.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        // 설정된 키가 충분히 긴지 확인
        String secret = jwtConfig.getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // HS512에 필요한 최소 64바이트(512비트) 확인
        if (keyBytes.length < 64) {
            log.warn("JWT 시크릿 키가 너무 짧습니다. 안전한 키를 생성합니다.");
            // 안전한 키 자동 생성
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰에서 사용자명 추출
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 토큰에서 만료일자 추출
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 토큰에서 클레임 추출
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 토큰에서 모든 클레임 추출
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 사용자 정보로 토큰 생성 (사용자 정보 포함)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // CustomUserDetails에서 추가 정보 포함
        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUser = (CustomUserDetails) userDetails;
            claims.put("userId", customUser.getUsername());
            claims.put("userName", customUser.getUserName());
            claims.put("userSeCd", customUser.getUserSeCd());
            claims.put("authorities", customUser.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList()));
        }

        return createToken(claims, userDetails.getUsername());
    }

    // 토큰 생성
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshExpiration());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰에서 사용자 정보 추출
    public Map<String, Object> getUserInfoFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Map<String, Object> userInfo = new HashMap<>();

            userInfo.put("userId", claims.getSubject());
            userInfo.put("userName", claims.get("userName"));
            userInfo.put("userSeCd", claims.get("userSeCd"));
            userInfo.put("authorities", claims.get("authorities"));

            return userInfo;
        } catch (Exception e) {
            log.error("토큰에서 사용자 정보 추출 오류: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // 토큰 유효성 검사
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 유효성 검사 (간단 버전)
    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // Request Header에서 토큰 추출
    public String getTokenFromHeader(String header) {
        if (header != null && header.startsWith(jwtConfig.getPrefix())) {
            return header.substring(jwtConfig.getPrefix().length());
        }
        return null;
    }
}