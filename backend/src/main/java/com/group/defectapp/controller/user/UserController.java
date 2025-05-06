package com.group.defectapp.controller.user;

import com.group.defectapp.dto.defect.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
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
    @GetMapping("/list")
    public ResponseEntity<Page<UserListDto>> getAllUsers(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userSeCd,
            @RequestParam Map<String, Object> paramMap
    ) {

        int pageIndex = Integer.parseInt(Objects.toString(paramMap.get("pageIndex")));
        int pageSize = Integer.parseInt(Objects.toString(paramMap.get("pageSize")));

        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();

        UserSearchCondition condition = UserSearchCondition.builder()
                .userId(userId)
                .userName(userName)
                .userSeCd(userSeCd)
                .build();

        Page<UserListDto> userList = userService.getUsersList(condition, pageRequestDto);
        return ResponseEntity.ok(userList);
    }

    /**
     * 사용자 정보 수정
     *
     * @param userRequestDto
     * @return
     */
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
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUsers(@RequestBody List<String> userIds) {
        userService.deleteUsers(userIds);
        return ResponseEntity.ok("사용자 정보 삭제 성공");
    }
}