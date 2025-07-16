package com.group.defectapp.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthServiceTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "jm0820@test.co.kr";
    private static final String TEST_PASSWORD = "testPassword123!";
    private static final String TEST_USER_NAME = "인증테스트사용자";
    private static final String TEST_USER_TYPE = "DP";

    private HttpHeaders headers;

    @BeforeAll
    @Transactional
    void setUp() {
        User testUser = User.builder()
                .userId(TEST_USER_ID)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .userName(TEST_USER_NAME)
                .userSeCd(TEST_USER_TYPE)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @Order(1)
    @DisplayName("유효한 자격 증명으로 로그인하면 성공해야 한다.")
    void login_success_with_valid_credentials() {
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .userId(TEST_USER_ID)
                .password(TEST_PASSWORD)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequestDto> request = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/auth/sign-in", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 쿠키 저장
        this.headers = new HttpHeaders();
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        cookies.forEach(cookie -> this.headers.add(HttpHeaders.COOKIE, cookie));

        // 응답 JSON 검증
        assertTrue(response.getBody().contains("\"result\":\"success\""));
        assertTrue(response.getBody().contains(TEST_USER_ID));
    }

    @Test
    @Order(2)
    @DisplayName("로그인 후 현재 사용자 정보를 조회할 수 있어야 한다.")
    void me_should_return_user_info() {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/auth/me", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(TEST_USER_ID));
    }

    @Test
    @Order(3)
    @DisplayName("로그인 후 토큰 유효성 검사에 성공해야 한다.")
    void token_check_should_be_valid() {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/auth/check-token", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"valid\":true"));
    }

    @Test
    @Order(4)
    @DisplayName("로그아웃 후 보호된 리소스 접근은 실패해야 한다.")
    void logout_should_block_access() {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        restTemplate.exchange("/auth/logout", HttpMethod.POST, request, String.class);

        ResponseEntity<String> response = restTemplate.exchange("/auth/me", HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(5)
    @DisplayName("잘못된 비밀번호로 로그인하면 실패해야 한다.")
    void login_fail_with_wrong_password() {
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .userId(TEST_USER_ID)
                .password("wrong-password")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequestDto> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/sign-in", request, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("error"));
    }
}
