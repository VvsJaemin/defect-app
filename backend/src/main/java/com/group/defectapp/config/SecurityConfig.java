package com.group.defectapp.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private static final String USER_ROLE = "ROLE_MG";
    private static final String LOGOUT_URL = "/auth/logout";
    private static final String INVALID_SESSION_URL = "/auth/login";
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
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(AUTH_WHITELIST).permitAll()
                    .requestMatchers("/users/**").hasAuthority(USER_ROLE)
                    .anyRequest().authenticated()
            )
            .logout(logout -> logout
                    .logoutUrl(LOGOUT_URL)
                    .logoutSuccessHandler((req, res, auth)
                            -> res.setStatus(HttpServletResponse.SC_OK))
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies(DELETE_COOKIES)
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .invalidSessionUrl(INVALID_SESSION_URL)
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
            )
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()));

        return http.build();
    }
}