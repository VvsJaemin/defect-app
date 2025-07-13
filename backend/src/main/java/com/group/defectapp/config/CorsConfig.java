
package com.group.defectapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final String[] allowedOrigins = {"http://localhost:5173", "https://qms.jaemin.app"};


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 쿠키 기반 인증을 위해 반드시 필요
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        // 환경별 Origin 설정
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));

        // 모든 HTTP 메서드 허용
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // 모든 헤더 허용
        config.setAllowedHeaders(List.of("*"));

        // 클라이언트가 접근할 수 있는 응답 헤더 설정
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Set-Cookie"  // 쿠키 설정을 위해 추가
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}