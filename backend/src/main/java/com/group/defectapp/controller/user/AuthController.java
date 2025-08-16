package com.group.defectapp.controller.user;

import com.group.defectapp.config.CustomUserDetails;
import com.group.defectapp.config.CustomUserDetailsService;
import com.group.defectapp.dto.user.LoginRequestDto;
import com.group.defectapp.service.user.UserService;
import com.group.defectapp.util.CookieUtil;
import com.group.defectapp.util.JwtUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "사용자 인증", description = "사용자 인증 및 관리 API")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    // 비밀번호 실패 한계값 (예: 5회)
    private static final int PASSWORD_FAIL_LIMIT = 5;
    private static final String PASSWORD_FAIL_LIMIT_MSG = "비밀번호를 5회 이상 틀렸습니다.\n관리자에게 문의하세요.";
    private static final String LOGIN_FAIL_MSG = "아이디 또는 비밀번호가 올바르지 않습니다.";

    @Operation(
            summary = "사용자 로그인",
            description = "사용자 ID와 비밀번호를 통해 시스템에 로그인합니다. 성공 시 JWT 토큰을 쿠키에 설정합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                                "result": "success",
                                                "userId": "USER001",
                                                "userName": "김철수",
                                                "userSeCd": "DP",
                                                "authorities": ["ROLE_DP"]
                                            }
                                            """
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "개발자 로그인 성공",
                                            summary = "개발자 권한 사용자 로그인",
                                            value = """
                                                    {
                                                        "result": "success",
                                                        "userId": "DEV001",
                                                        "userName": "김개발",
                                                        "userSeCd": "DP",
                                                        "authorities": ["ROLE_DP"]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "관리자 로그인 성공",
                                            summary = "관리자 권한 사용자 로그인",
                                            value = """
                                                    {
                                                        "result": "success",
                                                        "userId": "ADMIN001",
                                                        "userName": "관리자",
                                                        "userSeCd": "MG",
                                                        "authorities": ["ROLE_MG"]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "QA 로그인 성공",
                                            summary = "품질보증 담당자 로그인",
                                            value = """
                                                    {
                                                        "result": "success",
                                                        "userId": "QA001",
                                                        "userName": "김품질",
                                                        "userSeCd": "QA",
                                                        "authorities": ["ROLE_QA"]
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "로그인 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = """
                                            {
                                                "error": "LOGIN_FAILED",
                                                "message": "아이디 또는 비밀번호가 올바르지 않습니다."
                                            }
                                            """
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "로그인 실패",
                                            summary = "잘못된 인증 정보",
                                            value = """
                                                    {
                                                        "error": "LOGIN_FAILED",
                                                        "message": "아이디 또는 비밀번호가 올바르지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "계정 잠금",
                                            summary = "비밀번호 5회 이상 실패",
                                            value = """
                                                    {
                                                        "error": "ACCOUNT_LOCKED", 
                                                        "message": "비밀번호를 5회 이상 틀렸습니다.\\n관리자에게 문의하세요."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "필수 정보 누락",
                                    value = """
                                            {
                                                "error": "INVALID_REQUEST",
                                                "message": "사용자 ID 또는 비밀번호가 누락되었습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(
            @Parameter(
                    description = "로그인 인증 정보",
                    required = true,
                    schema = @Schema(implementation = LoginRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "개발자 로그인",
                                    summary = "개발팀 사용자 로그인",
                                    value = """
                                            {
                                                "userId": "DEV001",
                                                "password": "password123!"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "관리자 로그인",
                                    summary = "시스템 관리자 로그인",
                                    value = """
                                            {
                                                "userId": "ADMIN001", 
                                                "password": "admin123!"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "QA 로그인",
                                    summary = "품질보증팀 사용자 로그인",
                                    value = """
                                            {
                                                "userId": "QA001",
                                                "password": "qa123!"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "고객사 로그인",
                                    summary = "고객사 담당자 로그인",
                                    value = """
                                            {
                                                "userId": "CU001",
                                                "password": "customer123!"
                                            }
                                            """
                            )
                    }
            )
            @RequestBody LoginRequestDto loginRequest,
            HttpServletResponse response) {
        String userId = loginRequest.getUserId();

        try {
            // 1. 사용자 존재 여부 및 계정 잠금 상태 확인
            if (!userDetailsService.existsByUserId(userId)) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
            }

            // 2. 사용자 정보 조회하여 비밀번호 실패 횟수 확인
            var user = userService.findByUserId(userId);
            if (isOverPwdFailLimit(user.getPwdFailCnt())) {
                return ResponseEntity.status(401)
                        .body(createErrorResponse("ACCOUNT_LOCKED", PASSWORD_FAIL_LIMIT_MSG));
            }

            // 3. 인증 수행
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userId, loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 4. 로그인 성공 처리
            userService.resetPwdFailCnt(userId);  // 실패 카운트 초기화
            userService.updateLastLoginAt(userId);  // 마지막 로그인 시간 업데이트

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 쿠키 설정
            cookieUtil.setJwtCookies(response, accessToken, refreshToken);
            cookieUtil.setUserInfoCookie(response, userDetails);

            // 응답 데이터 구성
            Map<String, Object> body = new HashMap<>();
            body.put("result", "success");
            body.put("userId", userDetails.getUsername());
            body.put("userName", userDetails.getUserName());
            body.put("userSeCd", userDetails.getUserSeCd());
            body.put("authorities", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList()));

            log.info("로그인 성공: {}", userId);
            return ResponseEntity.ok(body);

        } catch (BadCredentialsException e) {
            // 5. 인증 실패 시 실패 카운트 증가
            try {
                int failCount = userService.updatePwdFailCnt(userId);
                String errorMessage = isOverPwdFailLimit(failCount) ? PASSWORD_FAIL_LIMIT_MSG : LOGIN_FAIL_MSG;
                String errorCode = isOverPwdFailLimit(failCount) ? "ACCOUNT_LOCKED" : "LOGIN_FAILED";

                log.warn("로그인 실패 - 비밀번호 오류: {} (실패 횟수: {})", userId, failCount);
                return ResponseEntity.status(401)
                        .body(createErrorResponse(errorCode, errorMessage));
            } catch (Exception ex) {
                log.error("로그인 실패 처리 중 오류", ex);
                return ResponseEntity.status(401)
                        .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
            }
        } catch (UsernameNotFoundException e) {
            log.warn("로그인 실패 - 사용자 없음: {}", userId);
            return ResponseEntity.status(401)
                    .body(createErrorResponse("LOGIN_FAILED", LOGIN_FAIL_MSG));
        } catch (Exception e) {
            log.error("로그인 처리 중 예상치 못한 오류", e);
            return ResponseEntity.status(401)
                    .body(createErrorResponse("LOGIN_FAILED", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @Operation(
            summary = "토큰 갱신",
            description = "만료된 액세스 토큰을 리프레시 토큰을 사용하여 갱신합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "갱신 성공",
                                    value = """
                                            {
                                                "result": "success"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "토큰 갱신 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 리프레시 토큰",
                                            value = """
                                                    {
                                                        "error": "INVALID_TOKEN",
                                                        "message": "리프레시 토큰이 유효하지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "토큰 갱신 실패",
                                            value = """
                                                    {
                                                        "error": "REFRESH_FAILED",
                                                        "message": "리프레시 토큰 갱신에 실패하였습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = cookieUtil.getRefreshTokenFromCookies(request);
            if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(createErrorResponse("INVALID_TOKEN", "리프레시 토큰이 유효하지 않습니다."));
            }

            String userId = jwtUtil.getUsernameFromToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            String newAccessToken = jwtUtil.generateToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            // 쿠키 갱신
            cookieUtil.setJwtCookies(response, newAccessToken, newRefreshToken);
            cookieUtil.setUserInfoCookie(response, (CustomUserDetails) userDetails);

            Map<String, Object> body = new HashMap<>();
            body.put("result", "success");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("리프레시 토큰 실패", e);
            return ResponseEntity.status(401).body(createErrorResponse("REFRESH_FAILED", "리프레시 토큰 갱신에 실패하였습니다."));
        }
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 사용자를 로그아웃하고 인증 관련 쿠키를 삭제합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "로그아웃 성공",
                                    value = """
                                            {
                                                "message": "로그아웃되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtil.deleteAuthCookies(response);
        return ResponseEntity.ok(createSuccessResponse("로그아웃되었습니다."));
    }

    @Operation(
            summary = "현재 사용자 정보 조회",
            description = "현재 인증된 사용자의 기본 정보를 조회합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "개발자 정보",
                                            summary = "개발자 사용자 정보",
                                            value = """
                                                    {
                                                        "userId": "DEV001",
                                                        "userName": "김개발",
                                                        "userSeCd": "DP",
                                                        "authorities": ["ROLE_DP"]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "관리자 정보",
                                            summary = "관리자 사용자 정보",
                                            value = """
                                                    {
                                                        "userId": "ADMIN001",
                                                        "userName": "관리자",
                                                        "userSeCd": "MG",
                                                        "authorities": ["ROLE_MG"]
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "인증 실패",
                                    value = """
                                            {
                                                "error": "UNAUTHORIZED",
                                                "message": "인증되지 않았습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body(createErrorResponse("UNAUTHORIZED", "인증되지 않았습니다."));
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userDetails.getUsername());
        body.put("userName", userDetails.getUserName());
        body.put("userSeCd", userDetails.getUserSeCd());
        body.put("authorities", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "토큰 유효성 검사",
            description = "현재 액세스 토큰의 유효성을 검사합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 유효함",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "토큰 유효",
                                    value = """
                                            {
                                                "valid": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "토큰 무효함",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "토큰 무효",
                                    value = """
                                            {
                                                "error": "INVALID_TOKEN",
                                                "message": "토큰이 유효하지 않습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/check-token")
    public ResponseEntity<?> checkToken(HttpServletRequest request) {
        String accessToken = cookieUtil.getAccessTokenFromCookies(request);
        if (accessToken == null || !jwtUtil.validateToken(accessToken)) {
            return ResponseEntity.status(401).body(createErrorResponse("INVALID_TOKEN", "토큰이 유효하지 않습니다."));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("valid", true);
        return ResponseEntity.ok(body);
    }

    @Hidden
    @Operation(
            summary = "계정 잠금 해제",
            description = "비밀번호 실패로 잠긴 계정을 해제합니다. 관리자 권한이 필요합니다.",
            tags = {"인증"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "계정 잠금 해제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "해제 성공",
                                    value = """
                                            {
                                                "message": "USER001 계정이 잠금 해제되었습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "사용자 ID 누락",
                                    value = """
                                            {
                                                "error": "INVALID_REQUEST",
                                                "message": "사용자 ID가 필요합니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "해제 실패",
                                    value = """
                                            {
                                                "error": "UNLOCK_FAILED",
                                                "message": "계정 잠금 해제 중 오류가 발생했습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/unlock-account")
    public ResponseEntity<?> unlockAccount(
            @Parameter(
                    description = "계정 잠금 해제 요청",
                    required = true,
                    examples = @ExampleObject(
                            name = "계정 잠금 해제 요청",
                            value = """
                                    {
                                        "userId": "USER001"
                                    }
                                    """
                    )
            )
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("INVALID_REQUEST", "사용자 ID가 필요합니다."));
            }

            // 계정 잠금 해제 (실패 카운트 초기화)
            userService.resetPwdFailCnt(userId.trim());
            log.info("계정 잠금 해제: {}", userId);

            return ResponseEntity.ok(createSuccessResponse(userId + " 계정이 잠금 해제되었습니다."));
        } catch (Exception e) {
            log.error("계정 잠금 해제 중 오류", e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("UNLOCK_FAILED", "계정 잠금 해제 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgetPassword(@RequestBody Map<String, Object> userInfo) {

        String userId = Objects.toString(userInfo.get("email"));

        String retForgotPassword = userService.forgetPassword(userId);

        return ResponseEntity.ok(retForgotPassword);
    }

    /**
     * 비밀번호 실패 횟수가 제한을 초과했는지 확인
     */
    private boolean isOverPwdFailLimit(int failCount) {
        return failCount >= PASSWORD_FAIL_LIMIT;
    }

    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", error);
        map.put("message", message);
        return map;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }
}