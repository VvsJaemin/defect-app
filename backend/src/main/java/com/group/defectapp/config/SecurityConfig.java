package com.group.defectapp.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
            "/auth/**", "/login", "/signup",
            "/swagger-ui/**", "/v3/api-docs/**",
            "/my-swagger-ui", "/my-api-docs"
    };

    private static final String[] DELETE_COOKIES = {"JSESSIONID"};

    private final CorsConfig corsConfig;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS 설정 적용 (가장 먼저)
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

            // 2. CSRF 비활성화
            .csrf(csrf -> csrf.disable())

            // 3. 권한 설정
            .authorizeHttpRequests(auth -> auth
                // Preflight OPTIONS 요청은 모두 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 인증이 필요없는 경로 허용
                .requestMatchers(AUTH_WHITELIST).permitAll()

                // 그 외 경로는 인증 필요
                .anyRequest().authenticated()
            )

            // 4. 로그아웃 설정
            .logout(logout -> logout
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(DELETE_COOKIES)
            )

            // 5. 세션 관리 (필요시 생성)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }
}
