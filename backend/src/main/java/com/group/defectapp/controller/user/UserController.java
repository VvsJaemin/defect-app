
package com.group.defectapp.controller.user;

import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.user.UserListDto;
import com.group.defectapp.dto.user.UserRequestDto;
import com.group.defectapp.dto.user.UserResponseDto;
import com.group.defectapp.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "사용자 관리", description = "결함관리시스템 사용자 관리 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "사용자 목록 조회",
            description = "결함관리시스템 사용자 목록을 페이징과 검색 조건으로 조회합니다. 관리자(MG) 권한이 필요합니다.",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserListDto.class),
                            examples = @ExampleObject(
                                    name = "사용자 목록 응답",
                                    value = """
                                            {
                                                "content": [
                                                    {
                                                        "userId": "USER001",
                                                        "userName": "김철수",
                                                        "email": "kim@example.com",
                                                        "userSeCd": "DP",
                                                        "roleCode": "개발자",
                                                        "useYn": "Y",
                                                        "createdAt": "2024-01-15T09:30:00",
                                                        "lastLoginAt": "2024-01-16T08:15:00"
                                                    }
                                                ],
                                                "pageable": {
                                                    "pageNumber": 0,
                                                    "pageSize": 10
                                                },
                                                "totalElements": 25,
                                                "totalPages": 3,
                                                "first": true,
                                                "last": false
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "유효성 검사 실패",
                                    value = """
                                            {
                                                "error": "BAD_REQUEST",
                                                "message": "페이지 크기는 1 이상 100 이하여야 합니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('MG')")
    @GetMapping("/list")
    public ResponseEntity<Page<UserListDto>> getAllUsers(
            @Parameter(
                    name = "pageRequestDto",
                    description = "페이징 및 정렬 정보",
                    required = true,
                    schema = @Schema(implementation = PageRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "기본 페이징",
                                    summary = "첫 번째 페이지 조회",
                                    value = """
                                            {
                                                "pageIndex": 1,
                                                "pageSize": 10
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "정렬 포함 페이징",
                                    summary = "사용자명 기준 오름차순 정렬",
                                    value = """
                                            {
                                                "pageIndex": 1,
                                                "pageSize": 20,
                                                "sortKey": "userName",
                                                "sortOrder": "asc"
                                            }
                                            """
                            )
                    }
            )
            @Validated PageRequestDto pageRequestDto,

            @Parameter(
                    name = "paramMap",
                    description = "사용자 검색 및 필터링 조건",
                    required = false,
                    schema = @Schema(
                            type = "object",
                            additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                            example = """
                                    {
                                        "userName": "김",
                                        "userSeCd": "DP",
                                        "useYn": "Y"
                                    }
                                    """
                    ),
                    examples = {
                            @ExampleObject(
                                    name = "사용자명 검색",
                                    summary = "사용자명으로 검색",
                                    value = """
                                            {
                                                "userName": "김철수"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "역할별 필터링",
                                    summary = "사용자 역할로 필터링",
                                    value = """
                                            {
                                                "userSeCd": "DP"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "복합 검색 조건",
                                    summary = "여러 조건으로 검색",
                                    value = """
                                            {
                                                "userName": "김",
                                                "userSeCd": "QA",
                                                "useYn": "Y"
                                            }
                                            """
                            )
                    }
            )
            @RequestParam(required = false) Map<String,Object> paramMap
    ) {
        Page<UserListDto> userList = userService.getUsersList(paramMap, pageRequestDto);
        return ResponseEntity.ok(userList);
    }

    @Operation(
            summary = "사용자 등록",
            description = "새로운 사용자를 결함관리시스템에 등록합니다 (회원가입).",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 등록 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "등록 성공",
                                    value = "사용자 등록이 성공했습니다."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "유효성 검증 실패",
                                    value = """
                                            {
                                                "error": "VALIDATION_FAILED",
                                                "message": "유효하지 않은 사용자 정보입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "중복된 사용자 ID"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @Parameter(
                    description = "사용자 등록 정보",
                    required = true,
                    examples = @ExampleObject(
                            name = "사용자 등록 요청",
                            value = """
                                    {
                                        "userId": "USER001",
                                        "userName": "김철수",
                                        "password": "password123!",
                                        "confirmPassword": "password123!",
                                        "email": "kim@example.com",
                                        "userSeCd": "DP",
                                        "useYn": "Y"
                                    }
                                    """
                    )
            )
            @Valid @RequestBody UserRequestDto userRequestDto
    ) {
        userService.save(userRequestDto);
        return ResponseEntity.ok("사용자 등록이 성공했습니다.");
    }

    @Operation(
            summary = "사용자 정보 수정",
            description = "기존 사용자의 정보를 수정합니다.",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "수정 성공",
                                    value = "사용자 정보 수정 성공했습니다."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 데이터",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "유효성 검증 실패",
                                    value = """
                                            {
                                                "error": "VALIDATION_FAILED",
                                                "message": "유효하지 않은 사용자 정보입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "수정할 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
    @PutMapping("/modifyUser")
    public ResponseEntity<String> modifyUser(
            @Parameter(
                    description = "수정할 사용자 정보",
                    required = true,
                    examples = @ExampleObject(
                            name = "사용자 수정 요청",
                            value = """
                                    {
                                        "userId": "USER001",
                                        "userName": "김철수",
                                        "email": "kim.updated@example.com",
                                        "userSeCd": "QA",
                                        "useYn": "Y"
                                    }
                                    """
                    )
            )
            @Valid @RequestBody UserRequestDto userRequestDto
    ) {
        userService.updateUser(userRequestDto);
        return ResponseEntity.ok("사용자 정보 수정 성공했습니다.");
    }

    @Operation(
            summary = "사용자 상세 정보 조회",
            description = "사용자 ID를 통해 특정 사용자의 상세 정보를 조회합니다.",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponseDto.class),
                            examples = @ExampleObject(
                                    name = "사용자 상세 응답",
                                    value = """
                                            {
                                                "userId": "USER001",
                                                "userName": "김철수",
                                                "email": "kim@example.com",
                                                "userSeCd": "DP",
                                                "roleCode": "개발자",
                                                "useYn": "Y",
                                                "createdAt": "2024-01-15T09:30:00",
                                                "updatedAt": "2024-01-16T14:20:00",
                                                "lastLoginAt": "2024-01-16T08:15:00",
                                                "loginFailCount": 0
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "사용자 없음",
                                    value = """
                                            {
                                                "error": "USER_NOT_FOUND",
                                                "message": "사용자를 찾을 수 없습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
    @GetMapping("/read")
    public ResponseEntity<UserResponseDto> readUser(
            @Parameter(
                    description = "조회할 사용자의 고유 ID",
                    required = true,
                    example = "USER001",
                    schema = @Schema(pattern = "[A-Z0-9]{6,20}")
            )
            @RequestParam String userId
    ) {
        UserResponseDto userResponseDto = userService.readUser(userId);
        return ResponseEntity.ok(userResponseDto);
    }

    @Operation(
            summary = "사용자 정보 삭제",
            description = "선택한 사용자들을 시스템에서 삭제합니다. 관리자(MG) 권한이 필요합니다.",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "삭제 성공",
                                    value = "사용자 정보 삭제 성공"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "삭제할 수 없는 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "삭제 불가",
                                    value = """
                                            {
                                                "error": "DELETE_NOT_ALLOWED",
                                                "message": "삭제할 수 없는 사용자입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "삭제할 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('MG')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUsers(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "삭제할 사용자 ID 목록",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string")),
                            examples = {
                                    @ExampleObject(
                                            name = "단일 사용자 삭제",
                                            value = "[\"USER001\"]"
                                    ),
                                    @ExampleObject(
                                            name = "복수 사용자 삭제",
                                            value = "[\"USER001\", \"USER002\", \"USER003\"]"
                                    )
                            }
                    )
            )
            @RequestBody List<String> userIds
    ) {
        userService.deleteUsers(userIds);
        return ResponseEntity.ok("사용자 정보 삭제 성공");
    }

    @Operation(
            summary = "사용자 비밀번호 초기화",
            description = "사용자의 비밀번호 실패 횟수를 초기화합니다. 관리자(MG) 권한이 있는 사용자만 다른 사용자의 비밀번호를 초기화할 수 있습니다.",
            tags = {"사용자 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 초기화 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "초기화 성공",
                                    value = "비밀번호 실패 횟수가 초기화되었습니다."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "유효하지 않은 사용자 ID",
                                    value = "사용자 계정이 존재하지 않습니다."
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('MG')")
    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "비밀번호 초기화 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    additionalProperties = Schema.AdditionalPropertiesValue.TRUE
                            ),
                            examples = @ExampleObject(
                                    name = "비밀번호 초기화 요청",
                                    value = """
                                            {
                                                "userId": "USER001"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request
    ) {
        String userId = request.get("userId");

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("사용자 계정이 존재하지 않습니다.");
        }

        userService.resetPwdFailCnt(userId);
        return ResponseEntity.ok("비밀번호 실패 횟수가 초기화되었습니다.");
    }
}