package com.group.defectapp.controller.user;

import com.group.defectapp.domain.user.User;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_LOGIN_USER = "loginUser";

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto, HttpServletRequest request) {
        User user = userService.findByUserId(loginRequestDto.getUserId());

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 기존 세션이 있다면 제거하고 새로 생성
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession newSession = request.getSession(true);
        UserResponseDto responseDto = new UserResponseDto(user);
        newSession.setAttribute(SESSION_LOGIN_USER, responseDto);

        userService.updateLastLoginAt(user.getUserId());

        return ResponseEntity.ok(responseDto);

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("로그아웃 성공");
    }

    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object loginUser = (session != null) ? session.getAttribute(SESSION_LOGIN_USER) : null;
        if (loginUser != null) {
            return ResponseEntity.ok(loginUser);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인된 사용자가 없습니다.");
    }
}
