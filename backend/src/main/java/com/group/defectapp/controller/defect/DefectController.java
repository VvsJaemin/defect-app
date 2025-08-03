package com.group.defectapp.controller.defect;

import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.defect.*;
import com.group.defectapp.service.defect.DefectService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
@Tag(name = "결함 관리", description = "품질관리시스템 결함 관리 API")
@RestController
@RequestMapping("/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    @Operation(
            summary = "결함 등록",
            description = "새로운 결함을 시스템에 등록합니다. 파일 첨부가 가능합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 등록 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(
                                    name = "등록 성공",
                                    value = """
                                            {
                                                "message": "결함 등록이 성공했습니다.",
                                                "defectId": "DT0000000001"
                                            }
                                            """
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
                                                "message": "유효하지 않은 결함 정보입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> saveDefect(
            @Parameter(
                    description = "결함 등록 정보",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectRequestDto.class),
                            examples = @ExampleObject(
                                    name = "결함 등록 요청",
                                    value = """
                                            {
                                                "projectId": "PROJ0001",
                                                "assigneeId": "USER001",
                                                "statusCode": "DS1000",
                                                "seriousCode": "3",
                                                "orderCode": "IMPROVING",
                                                "defectDivCode": "FUNCTION",
                                                "defectTitle": "버튼 클릭 시 오류 발생",
                                                "defectMenuTitle": "메인 메뉴 > 환경설정",
                                                "defectUrlInfo": "https://qms.example.com/defect/1",
                                                "defectContent": "환경설정 저장 버튼을 클릭하면 500 에러 발생",
                                                "defectEtcContent": "재현 조건: 관리자 계정 로그인 필수",
                                                "openYn": "Y"
                                            }
                                            """
                            )
                    )
            )
            @RequestPart("defectRequestDto") DefectRequestDto defectRequestDto,

            @Parameter(
                    description = "첨부파일 (선택사항)",
                    required = false,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "files", required = false) MultipartFile[] files,

            Principal principal) {

        defectService.saveDefect(defectRequestDto, files, principal.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "결함 목록 조회",
            description = "페이징, 정렬, 검색 조건을 포함하여 결함 목록을 조회합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectListDto.class),
                            examples = @ExampleObject(
                                    name = "결함 목록 응답",
                                    value = """
                                            {
                                                "content": [
                                                    {
                                                        "defectId": "DT0000000001",
                                                        "projectId": "PROJ0001",
                                                        "defectTitle": "버튼 클릭 시 오류 발생",
                                                        "statusCode": "DS1000",
                                                        "seriousCode": "3",
                                                        "orderCode": "IMPROVING",
                                                        "defectDivCode": "FUNCTION",
                                                        "assigneeId": "USER001",
                                                        "assigneeName": "김철수",
                                                        "createdAt": "2024-01-15T09:30:00",
                                                        "createdBy": "USER001",
                                                        "openYn": "Y"
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
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/list")
    public ResponseEntity<Page<DefectListDto>> listDefects(
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
                                    summary = "생성일 기준 내림차순 정렬",
                                    value = """
                                            {
                                                "pageIndex": 1,
                                                "pageSize": 20,
                                                "sortKey": "createdAt",
                                                "sortOrder": "desc"
                                            }
                                            """
                            )
                    }
            )
            @Validated PageRequestDto pageRequestDto,

            @Parameter(
                    name = "paramMap",
                    description = "결함 검색 및 필터링 조건",
                    required = false,
                    schema = @Schema(
                            type = "object",
                            additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
                            example = """
                                    {
                                        "defectTitle": "버튼",
                                        "statusCode": "DS1000",
                                        "projectId": "PROJ0001",
                                        "assigneeId": "USER001"
                                    }
                                    """
                    ),
                    examples = {
                            @ExampleObject(
                                    name = "제목 검색",
                                    summary = "결함 제목으로 검색",
                                    value = """
                                            {
                                                "defectTitle": "버튼 오류"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "상태별 필터링",
                                    summary = "결함 상태로 필터링",
                                    value = """
                                            {
                                                "statusCode": "DS1000"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "복합 검색 조건",
                                    summary = "여러 조건으로 검색",
                                    value = """
                                            {
                                                "projectId": "PROJ0001",
                                                "statusCode": "DS2000",
                                                "seriousCode": "4",
                                                "defectDivCode": "FUNCTION"
                                            }
                                            """
                            )
                    }
            )
            @RequestParam(required = false) Map<String, Object> paramMap) {
        Page<DefectListDto> defectResponseList = defectService.defectList(pageRequestDto, paramMap);
        return ResponseEntity.ok(defectResponseList);
    }

    @Operation(
            summary = "결함 상세 조회",
            description = "결함 ID를 통해 특정 결함의 상세 정보를 조회합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectResponseDto.class),
                            examples = @ExampleObject(
                                    name = "결함 상세 응답",
                                    value = """
                                            {
                                                "defectId": "DT0000000001",
                                                "projectId": "PROJ0001",
                                                "statusCode": "DS1000",
                                                "seriousCode": "3",
                                                "orderCode": "IMPROVING",
                                                "defectDivCode": "FUNCTION",
                                                "defectTitle": "버튼 클릭 시 오류 발생",
                                                "defectMenuTitle": "메인 메뉴 > 환경설정",
                                                "defectUrlInfo": "https://qms.example.com/defect/1",
                                                "defectContent": "환경설정 저장 버튼을 클릭하면 500 에러 발생",
                                                "defectEtcContent": "재현 조건: 관리자 계정 로그인 필수",
                                                "createdAt": "2024-01-15T09:30:00",
                                                "createdBy": "USER001",
                                                "updatedAt": "2024-01-16T14:20:00",
                                                "updatedBy": "USER002",
                                                "openYn": "Y",
                                                "assigneeId": "USER001",
                                                "attachmentFiles": [
                                                    {
                                                        "fileId": "LOG001_0",
                                                        "orgFileName": "screenshot.png",
                                                        "sysFileName": "20240115_screenshot.png"
                                                    }
                                                ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결함을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "결함 없음",
                                    value = """
                                            {
                                                "error": "DEFECT_NOT_FOUND",
                                                "message": "결함을 찾을 수 없습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/read/{defectId}")
    public ResponseEntity<DefectResponseDto> readDefect(
            @Parameter(
                    description = "조회할 결함의 고유 ID",
                    required = true,
                    example = "DT0000000001",
                    schema = @Schema(pattern = "DT[0-9]{10}")
            )
            @PathVariable String defectId) {
        DefectResponseDto defectResponseDto = defectService.readDefect(defectId);
        return ResponseEntity.ok(defectResponseDto);
    }

    @Operation(
            summary = "결함 정보 수정",
            description = "기존 결함의 정보를 수정합니다. 파일 첨부 수정도 가능합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "수정 성공",
                                    value = "결함 수정이 성공했습니다."
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
                                                "message": "유효하지 않은 결함 정보입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "수정할 결함을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PutMapping(value = "/modify-defects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> modifyDefect(
            @Parameter(
                    description = "수정할 결함 정보",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectRequestDto.class),
                            examples = @ExampleObject(
                                    name = "결함 수정 요청",
                                    value = """
                                            {
                                                "defectId": "DT0000000001",
                                                "projectId": "PROJ0001",
                                                "assigneeId": "USER002",
                                                "statusCode": "DS2000",
                                                "seriousCode": "4",
                                                "orderCode": "MOMETLY",
                                                "defectDivCode": "SYSTEM",
                                                "defectTitle": "수정된 결함 제목",
                                                "defectMenuTitle": "메인 메뉴 > 환경설정",
                                                "defectUrlInfo": "https://qms.example.com/defect/1",
                                                "defectContent": "수정된 결함 내용",
                                                "defectEtcContent": "수정된 기타 내용",
                                                "openYn": "Y"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestPart("defectRequestDto") DefectRequestDto defectRequestDto,

            @Parameter(
                    description = "첨부파일 (선택사항)",
                    required = false,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "files", required = false) MultipartFile[] files,

            Principal principal) {
        defectService.modifyDefect(defectRequestDto, files, principal);
        return ResponseEntity.ok("결함 수정이 성공했습니다.");
    }

    @Operation(
            summary = "결함 삭제",
            description = "선택한 결함을 시스템에서 삭제합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "삭제 성공",
                                    value = "등록하신 결함을 정상적으로 삭제했습니다."
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "삭제할 결함을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "결함 없음",
                                    value = """
                                            {
                                                "error": "DEFECT_NOT_FOUND",
                                                "message": "삭제할 결함을 찾을 수 없습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{defectId}")
    public ResponseEntity<String> deleteDefect(
            @Parameter(
                    description = "삭제할 결함의 고유 ID",
                    required = true,
                    example = "DT0000000001",
                    schema = @Schema(pattern = "DT[0-9]{10}")
            )
            @PathVariable String defectId, Principal principal) {
        defectService.deleteDefect(defectId, principal);
        return ResponseEntity.ok("등록하신 결함을 정상적으로 삭제했습니다.");
    }

    @Operation(
            summary = "결함 프로젝트 목록 조회",
            description = "결함 관리에서 사용할 수 있는 프로젝트 목록을 조회합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = DefectProjectListDto.class)),
                            examples = @ExampleObject(
                                    name = "프로젝트 목록 응답",
                                    value = """
                                            [
                                                {
                                                    "projectId": "PROJ0001",
                                                    "projectName": "QMS 시스템 개발",
                                                    "customerName": "삼성전자",
                                                    "statusCode": "DEV",
                                                    "useYn": "Y"
                                                },
                                                {
                                                    "projectId": "PROJ002",
                                                    "projectName": "ERP 시스템 구축",
                                                    "customerName": "LG전자",
                                                    "statusCode": "OPERATE",
                                                    "useYn": "Y"
                                                }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/projectList")
    public ResponseEntity<List<DefectProjectListDto>> defectProjectList() {
        List<DefectProjectListDto> defectProjectList = defectService.defectProjectList();
        return ResponseEntity.ok(defectProjectList);
    }

    @Operation(
            summary = "결함 대시보드 데이터 조회",
            description = "결함 관리 대시보드에 표시할 통계 데이터를 조회합니다.",
            tags = {"결함 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "대시보드 데이터 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectDashBoardDto.class),
                            examples = @ExampleObject(
                                    name = "대시보드 데이터 응답",
                                    value = """
                                            {
                                                "totalDefectCount": 125,
                                                "openDefectCount": 48,
                                                "closedDefectCount": 77,
                                                "criticalDefectCount": 12,
                                                "highDefectCount": 23,
                                                "mediumDefectCount": 56,
                                                "lowDefectCount": 34,
                                                "statusStatistics": {
                                                    "DS1000": 15,
                                                    "DS2000": 23,
                                                    "DS3000": 35,
                                                    "DS5000": 52
                                                },
                                                "typeStatistics": {
                                                    "FUNCTION": 45,
                                                    "UI": 32,
                                                    "SYSTEM": 28,
                                                    "DOCUMENT": 20
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/dashboard/list")
    public ResponseEntity<DefectDashBoardDto> defectDashBoardList() {
        DefectDashBoardDto defectDashBoardDto = defectService.defectDashBoardList();
        return ResponseEntity.ok(defectDashBoardDto);
    }

}
