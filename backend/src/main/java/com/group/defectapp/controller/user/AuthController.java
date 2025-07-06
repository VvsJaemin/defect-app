package com.group.defectapp.controller.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.exception.user.UserException;
import com.group.defectapp.service.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_LOGIN_USER = "loginUser";
    private static final String LOGIN_FAIL_MSG = "아이디 또는 비밀번호가 일치하지 않습니다.";
    private static final String PASSWORD_FAIL_LIMIT_MSG = "비밀번호를 5회 이상 틀렸습니다. 관리자에게 문의하세요.";
    private static final String SESSION_FAIL_MSG = "로그인된 사용자가 없습니다.";
    private static final String LOGOUT_SUCCESS_MSG = "로그아웃 성공";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 로그인 요청 처리.
     * 성공 시 세션을 생성하고 인증 정보를 저장.
     *
     * @param dto 로그인 요청 정보 (userId, password)
     * @param request         HTTP 요청 객체
     * @return 인증 성공 시 UserResponseDto, 실패 시 에러 메시지
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto dto, HttpServletRequest request) {
        try {
            User user = userService.findByUserId(dto.getUserId());

            if(!user.getUserSeCd().equals("MG")) {
                if (isOverPwdFailLimit(user.getPwdFailCnt())) {
                    return unauthorized(PASSWORD_FAIL_LIMIT_MSG);
                }
            }

            if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                if(!user.getUserSeCd().equals("MG")) {
                    int failCount = userService.updatePwdFailCnt(dto.getUserId());
                    return unauthorized(isOverPwdFailLimit(failCount) ? PASSWORD_FAIL_LIMIT_MSG : LOGIN_FAIL_MSG);
                } else {
                    return unauthorized(LOGIN_FAIL_MSG);
                }
            }

            setAuthentication(user, request);

            userService.resetPwdFailCnt(dto.getUserId());
            userService.updateLastLoginAt(user.getUserId());

            return ResponseEntity.ok(new UserResponseDto(user));
        } catch (UserException e) {
            return unauthorized(LOGIN_FAIL_MSG);
        }
    }


    /**
     * 사용자 로그아웃 처리.
     * 스프링 시큐리티 컨텍스트 및 세션 초기화.
     *
     * @param request HTTP 요청 객체
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        invalidateSession(request);
        deleteJsessionIdCookie(response);
        return ResponseEntity.ok(LOGOUT_SUCCESS_MSG);
    }


    /**
     * 현재 로그인된 사용자의 세션 정보 확인.
     * 세션이 없거나 만료된 경우 401 반환.
     *
     * @param request HTTP 요청 객체
     * @return 로그인 중인 사용자 정보 or 실패 메시지
     */
    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object loginUser = (session != null) ? session.getAttribute(SESSION_LOGIN_USER) : null;
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(SESSION_FAIL_MSG);
        }
        return ResponseEntity.ok(loginUser);
    }

    /**
     * 인증 정보를 기반으로 SecurityContext와 세션 속성을 설정.
     *
     * @param user    로그인 유저 엔티티
     * @param request HTTP 요청 객체
     */
    private void setAuthentication(User user, HttpServletRequest request) {
        // 기존 세션 무효화 후 새로 생성
        invalidateSession(request);
        HttpSession newSession = request.getSession(true);

        // 인증 객체 및 권한 설정
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUserId(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserSeCd()))
        );

        // SecurityContext에 인증 정보 저장 후 세션에 반영
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);

        newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        newSession.setAttribute(SESSION_LOGIN_USER, new UserResponseDto(user));
    }

    /**
     * 현재 세션이 존재할 경우 무효화(로그아웃).
     *
     * @param request HTTP 요청 객체
     */
    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private void deleteJsessionIdCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }


    private boolean isOverPwdFailLimit(int count) {
        return count >= 5;
    }

    private ResponseEntity<String> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }


}