package com.group.defectapp.controller.project;

import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.project.ProjectRequestDto;
import com.group.defectapp.dto.project.ProjectResponseDto;
import com.group.defectapp.dto.project.ProjectUserListDto;
import com.group.defectapp.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "프로젝트 관리", description = "품질관리시스템 프로젝트 관리 API")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('CU', 'DM', 'DP', 'MG', 'QA')")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
            summary = "프로젝트 등록",
            description = "새로운 품질관리 프로젝트를 시스템에 등록합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 등록 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "프로젝트 등록이 성공했습니다."
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
                                                "message": "유효하지 않은 데이터입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "409", description = "프로젝트명 중복")
    })
    @PostMapping("/save")
    public ResponseEntity<String> saveProject(
            @Parameter(
                    description = "프로젝트 등록 정보",
                    required = true,
                    examples = @ExampleObject(
                            name = "프로젝트 등록 요청",
                            value = """
                                    {
                                        "projectId": "PROJ0001",
                                        "projectName": "QMS 시스템 개발",
                                        "urlInfo": "https://qms.example.com",
                                        "customerName": "삼성전자",
                                        "statusCode": "DEV",
                                        "etcInfo": "품질관리시스템 개발 프로젝트",
                                        "useYn": "Y"
                                    }
                                    """
                    )
            )
            @Valid @RequestBody ProjectRequestDto projectRequestDto
    ) {
        projectService.saveProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 등록이 성공했습니다.");
    }

    @Operation(
            summary = "프로젝트 목록 조회",
            description = "페이징, 정렬, 검색 조건을 포함하여 프로젝트 목록을 조회합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectResponseDto.class),
                            examples = @ExampleObject(
                                    name = "프로젝트 목록 응답",
                                    summary = "성공적인 프로젝트 목록 조회 응답",
                                    description = "페이징된 프로젝트 목록과 메타데이터를 포함합니다.",
                                    value = """
                                        {
                                            "content": [
                                                {
                                                    "projectId": "PROJ0001",
                                                    "projectName": "QMS 시스템 개발",
                                                    "urlInfo": "https://qms.example.com",
                                                    "customerName": "삼성전자",
                                                    "statusCode": "DEV",
                                                    "statusName": "개발중",
                                                    "etcInfo": "품질관리시스템 개발 프로젝트",
                                                    "useYn": "Y",
                                                    "createdBy": "ADMIN001",
                                                    "createdByName": "관리자",
                                                    "updatedBy": "USER002",
                                                    "updatedByName": "이영희",
                                                    "createdAt": "2024-01-15T09:30:00",
                                                    "updatedAt": "2024-01-16T14:20:00",
                                                    "assignedUsers": ["USER001", "USER002"],
                                                    "assignedUsersMap": {
                                                        "USER001": "김철수",
                                                        "USER002": "이영희"
                                                    },
                                                    "assignedUserCnt": 2
                                                }
                                            ],
                                            "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 10,
                                                "sort": {
                                                    "sorted": true,
                                                    "orders": [
                                                        {
                                                            "property": "createdAt",
                                                            "direction": "DESC"
                                                        }
                                                    ]
                                                }
                                            },
                                            "totalElements": 25,
                                            "totalPages": 3,
                                            "first": true,
                                            "last": false,
                                            "numberOfElements": 10,
                                            "empty": false
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "유효성 검사 실패",
                                    value = """
                                        {
                                            "error": "BAD_REQUEST",
                                            "message": "페이지 크기는 10 이상 100 이하여야 합니다.",
                                            "timestamp": "2024-01-16T15:30:45"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/list")
    public ResponseEntity<Page<ProjectResponseDto>> listProjects(
            @Parameter(
                    name = "pageRequestDto",
                    description = "페이징 및 정렬 정보",
                    required = true,
                    schema = @Schema(implementation = PageRequestDto.class),
                    examples = {
                            @ExampleObject(
                                    name = "기본 페이징",
                                    summary = "첫 번째 페이지 조회",
                                    description = "기본 설정으로 첫 페이지를 조회합니다.",
                                    value = """
                                        {
                                            "pageIndex": 1,
                                            "pageSize": 10
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "정렬 포함 페이징",
                                    summary = "생성일 기준 내림차순 정렬",
                                    description = "생성일 기준으로 내림차순 정렬하여 조회합니다.",
                                    value = """
                                        {
                                            "pageIndex": 1,
                                            "pageSize": 20,
                                            "sortKey": "createdAt",
                                            "sortOrder": "desc"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "프로젝트명 정렬",
                                    summary = "프로젝트명 기준 오름차순 정렬",
                                    description = "프로젝트명을 기준으로 오름차순 정렬하여 조회합니다.",
                                    value = """
                                        {
                                            "pageIndex": 1,
                                            "pageSize": 10,
                                            "sortKey": "projectName",
                                            "sortOrder": "asc"
                                        }
                                        """
                            )
                    }
            )
            @Validated PageRequestDto pageRequestDto,

            @Parameter(
                    name = "paramMap",
                    description = "검색 및 필터링 조건",
                    required = false,
                    schema = @Schema(
                            type = "object",
                            additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                            example = """
                                {
                                    "projectName": "QMS",
                                    "customerName": "삼성",
                                    "statusCode": "DEV",
                                    "useYn": "Y"
                                }
                                """
                    ),
                    examples = {
                            @ExampleObject(
                                    name = "프로젝트명 검색",
                                    summary = "프로젝트명으로 검색",
                                    description = "프로젝트명에 특정 문자열이 포함된 프로젝트를 검색합니다.",
                                    value = """
                                        {
                                            "projectName": "QMS"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "고객사 및 상태 필터링",
                                    summary = "고객사와 상태로 필터링",
                                    description = "특정 고객사의 특정 상태 프로젝트만 조회합니다.",
                                    value = """
                                        {
                                            "customerName": "삼성전자",
                                            "statusCode": "DEV"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "복합 검색 조건",
                                    summary = "여러 조건으로 검색",
                                    description = "프로젝트명, 고객사, 상태, 사용여부 등 여러 조건으로 검색합니다.",
                                    value = """
                                        {
                                            "projectName": "시스템",
                                            "customerName": "삼성",
                                            "statusCode": "DEV",
                                            "useYn": "Y",
                                            "createdBy": "ADMIN"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "날짜 범위 검색",
                                    summary = "생성일 범위로 검색",
                                    description = "특정 기간에 생성된 프로젝트를 조회합니다.",
                                    value = """
                                        {
                                            "startDate": "2024-01-01",
                                            "endDate": "2024-01-31"
                                        }
                                        """
                            )
                    }
            )
            @RequestParam(required = false) Map<String, Object> paramMap
    ) {
        Page<ProjectResponseDto> projectResponseList = projectService.getProjectsList(pageRequestDto, paramMap);
        return ResponseEntity.ok(projectResponseList);
    }

    @Operation(
            summary = "프로젝트 상세 조회",
            description = "프로젝트 ID를 통해 특정 프로젝트의 상세 정보를 조회합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectResponseDto.class),
                            examples = @ExampleObject(
                                    name = "프로젝트 상세 응답",
                                    value = """
                                            {
                                                "projectId": "PROJ0001",
                                                "projectName": "QMS 시스템 개발",
                                                "urlInfo": "https://qms.example.com",
                                                "customerName": "삼성전자",
                                                "statusCode": "DEV",
                                                "etcInfo": "품질관리시스템 개발 프로젝트",
                                                "useYn": "Y",
                                                "createdBy": "ADMIN001",
                                                "updatedBy": "USER002",
                                                "createdAt": "2024-01-15 09:30:00",
                                                "updatedAt": "2024-01-16 14:20:00",
                                                "assignedUsers": ["USER001", "USER002", "USER003"],
                                                "assignedUsersMap": {
                                                    "USER001": "김철수",
                                                    "USER002": "이영희",
                                                    "USER003": "박민수"
                                                },
                                                "assignedUserCnt": 3
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "프로젝트를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "프로젝트 없음",
                                    value = """
                                            {
                                                "error": "PROJECT_NOT_FOUND",
                                                "message": "프로젝트를 찾을 수 없습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/read")
    public ResponseEntity<ProjectResponseDto> readProject(
            @Parameter(
                    description = "조회할 프로젝트의 고유 ID",
                    required = true,
                    example = "PROJ0001",
                    schema = @Schema(pattern = "PROJ[0-9]{4}")
            )
            @RequestParam String projectId
    ) {
        ProjectResponseDto projectResponseDto = projectService.readProject(projectId);
        return ResponseEntity.ok(projectResponseDto);
    }

    @Operation(
            summary = "프로젝트 정보 수정",
            description = "기존 프로젝트의 정보를 수정합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "수정 성공",
                                    value = "프로젝트 수정이 성공했습니다."
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
                                                "message": "유효하지 않은 데이터입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "수정할 프로젝트를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "409", description = "프로젝트명 중복")
    })
    @PostMapping("/modify-projects")
    public ResponseEntity<String> modifyProject(
            @Parameter(
                    description = "수정할 프로젝트 정보",
                    required = true,
                    examples = @ExampleObject(
                            name = "프로젝트 수정 요청",
                            value = """
                                    {
                                        "projectId": "PROJ0001",
                                        "projectName": "QMS 시스템 개발 v2.0",
                                        "urlInfo": "https://qms-v2.example.com",
                                        "customerName": "삼성전자",
                                        "statusCode": "OPERATE",
                                        "etcInfo": "품질관리시스템 고도화 프로젝트",
                                        "useYn": "Y"
                                    }
                                    """
                    )
            )
            @Valid @RequestBody ProjectRequestDto projectRequestDto
    ) {
        projectService.updateProject(projectRequestDto);
        return ResponseEntity.ok("프로젝트 수정이 성공했습니다.");
    }

    @Operation(
            summary = "프로젝트 삭제",
            description = "선택한 프로젝트들을 시스템에서 삭제합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "삭제 성공",
                                    value = "등록하신 프로젝트를 정상적으로 삭제했습니다."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "삭제할 수 없는 프로젝트",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "삭제 불가",
                                    value = """
                                        {
                                            "error": "DELETE_NOT_ALLOWED",
                                            "message": "삭제할 수 없는 프로젝트입니다."
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "삭제할 프로젝트를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteProject(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "삭제할 프로젝트 ID 목록",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string")),
                            examples = {
                                    @ExampleObject(
                                            name = "단일 프로젝트 삭제",
                                            value = "[\"PROJ0001\"]"
                                    ),
                                    @ExampleObject(
                                            name = "복수 프로젝트 삭제",
                                            value = "[\"PROJ0001\", \"PROJ0002\", \"PROJ0003\"]"
                                    )
                            }
                    )
            )
            @RequestBody List<String> projectIds
    ) {
        projectService.deleteProjects(projectIds);
        return ResponseEntity.ok("등록하신 프로젝트를 정상적으로 삭제했습니다.");
    }


    @Operation(
            summary = "프로젝트 할당 사용자 목록 조회",
            description = "특정 프로젝트에 할당된 사용자 목록을 조회합니다.",
            tags = {"프로젝트 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "array", implementation = ProjectUserListDto.class),
                            examples = @ExampleObject(
                                    name = "할당 사용자 목록",
                                    value = """
                                            [
                                                {
                                                    "userId": "USER001",
                                                    "userName": "김개발",
                                                    "userSeCd": "DP",
                                                    "roleName": "개발자",
                                                    "assignedDate": "2024-01-15",
                                                    "isActive": true
                                                },
                                                {
                                                    "userId": "USER002",
                                                    "userName": "박품질",
                                                    "userSeCd": "QA",
                                                    "roleName": "품질관리자",
                                                    "assignedDate": "2024-01-10",
                                                    "isActive": true
                                                }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/assignUserList")
    public ResponseEntity<List<ProjectUserListDto>> assignUserList(
            @Parameter(
                    description = "프로젝트 ID",
                    example = "PROJ0001",
                    schema = @Schema(pattern = "PROJ[0-9]{4}")
            )
            @RequestParam(required = false) String projectId
    ) {
        List<ProjectUserListDto> userList = projectService.assignProjectUserList(projectId);
        return ResponseEntity.ok(userList);
    }
}