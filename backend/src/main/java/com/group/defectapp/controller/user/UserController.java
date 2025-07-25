package com.group.defectapp.controller.user;

import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.dto.user.UserSearchCondition;
import com.group.defectapp.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 등록(회원가입)
     *
     * @param userRequestDto
     * @return
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody UserRequestDto userRequestDto) {
        userService.save(userRequestDto);
        return ResponseEntity.ok("사용자 등록이 성공했습니다.");
    }

    /**
     * 결함관리 시스템 사용자 목록 조회
     *
     * @param userId
     * @param userName
     * @param userSeCd
     * @param pageRequestDto
     * @return
     */
    @PreAuthorize("hasRole('MG')")
    @GetMapping("/list")
    public ResponseEntity<Page<UserListDto>> getAllUsers(
            @Validated PageRequestDto pageRequestDto,
            @RequestParam(required = false) Map<String,Object> paramMap
    ) {


        Page<UserListDto> userList = userService.getUsersList(paramMap, pageRequestDto);
        return ResponseEntity.ok(userList);
    }

    /**
     * 사용자 상세 정보
     * @param userId
     * @return
     */
    @PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
    @GetMapping("/read")
    public ResponseEntity<UserResponseDto> readUser(@RequestParam String userId) {
        UserResponseDto userResponseDto = userService.readUser(userId);
        return ResponseEntity.ok(userResponseDto);
    }


    /**
     * 사용자 정보 수정
     *
     * @param userRequestDto
     * @return
     */
    @PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
    @PutMapping("/modifyUser")
    public ResponseEntity<String> modifyUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        userService.updateUser(userRequestDto);
        return ResponseEntity.ok("사용자 정보 수정 성공했습니다.");
    }

    /**
     * 사용자 정보 삭제
     *
     * @param username
     * @return
     */
    @PreAuthorize("hasRole('MG')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUsers(@RequestBody List<String> userIds) {
        userService.deleteUsers(userIds);
        return ResponseEntity.ok("사용자 정보 삭제 성공");
    }

    /**
     * 사용자 비밀번호 초기화
     * 관리자(MG) 권한이 있는 사용자만 다른 사용자의 비밀번호를 초기화할 수 있음
     *
     * @param request 비밀번호 초기화 요청 (userId 포함)
     * @return 초기화 성공 메시지
     */
    @PreAuthorize("hasRole('MG')")
    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {

        String userId = request.get("userId");

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("사용자 계정이 존재하지 않습니다.");
        }

        userService.resetPwdFailCnt(userId);
        return ResponseEntity.ok("비밀번호 실패 횟수가 초기화되었습니다.");
    }


}