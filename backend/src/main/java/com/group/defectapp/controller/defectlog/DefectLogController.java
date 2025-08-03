package com.group.defectapp.controller.defectlog;

import com.group.defectapp.dto.common.PageRequestDto;
import com.group.defectapp.dto.defectlog.DefectLogListDto;
import com.group.defectapp.dto.defectlog.DefectLogRequestDto;
import com.group.defectapp.service.defectlog.DefectLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "결함 로그 관리", description = "결함 로그 생성, 조회, 파일 관리 API")
@RestController
@RequestMapping("/defectLogs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DefectLogController {

    private final DefectLogService defectLogService;

    @Operation(
            summary = "결함 로그 저장",
            description = "새로운 결함 로그를 생성하고 첨부파일이 있는 경우 함께 저장합니다.",
            tags = {"결함 로그 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 로그 저장 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "저장 성공",
                                    value = """
                                    {
                                        "id": 10001
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
                                "message": "필수 필드가 누락되었습니다."
                            }
                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "파일 크기 초과",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "파일 크기 초과",
                                    value = """
                            {
                                "error": "FILE_SIZE_EXCEEDED",
                                "message": "파일 크기가 허용된 제한을 초과했습니다."
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
    public ResponseEntity<?> save(
            @Parameter(
                    description = "결함 로그 저장 정보",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectLogRequestDto.class),
                            examples = @ExampleObject(
                                    name = "결함 로그 저장 요청",
                                    value = """
                            {
                                "defectId": "DEFECT-2024-001",
                                "logTitle": "결함 수정 완료",
                                "logCt": "결함 수정이 완료되었습니다. 테스트 진행 예정입니다.",
                                "statusCd": "DS1000",
                                "createdBy": "user01",
                                "assignUserId": "user02"
                            }
                            """
                            )
                    )
            )
            @Valid @RequestPart("defectLogRequestDto") DefectLogRequestDto defectLogRequestDto,

            @Parameter(
                    description = "첨부파일 목록 (선택사항, 최대 3개)",
                    required = false,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            examples = @ExampleObject(
                                    name = "첨부파일",
                                    description = "이미지, 문서 등의 파일을 첨부할 수 있습니다."
                            )
                    )
            )
            @RequestPart(value = "files", required = false) MultipartFile[] files
    ) {
        defectLogService.defectLogSave(defectLogRequestDto, files);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "결함 로그 목록 조회",
            description = "특정 결함 ID에 대한 결함 로그 목록을 페이징 처리하여 조회합니다.",
            tags = {"결함 로그 관리"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결함 로그 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DefectLogListDto.class),
                            examples = @ExampleObject(
                                    name = "결함 로그 목록 응답",
                                    value = """
                            {
                                "content": [
                                    {
                                        "logSeq": 1,
                                        "defectId": "DEFECT-2024-001",
                                        "logTitle": "결함 수정 완료",
                                        "logCt": "결함 수정이 완료되었습니다.",
                                        "statusCd": "DS1000",
                                        "createdAt": "2024-08-01 14:30:00",
                                        "createdBy": "user01",
                                        "assignUserId": "user02",
                                        "assignUserName": "홍길동",
                                        "defectTitle": "주문 처리 오류",
                                        "customerName": "ABC 회사",
                                        "defectLogFiles": []
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
                                "totalElements": 15,
                                "totalPages": 2,
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
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "유효성 검사 실패",
                                    value = """
                            {
                                "error": "BAD_REQUEST",
                                "message": "페이지 크기는 1 이상이어야 합니다."
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
                                "message": "해당 결함을 찾을 수 없습니다."
                            }
                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/list/{defectId}")
    public ResponseEntity<Page<DefectLogListDto>> listDefectLogs(
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
            @Validated @ModelAttribute PageRequestDto pageRequestDto,

            @Parameter(
                    description = "조회할 결함의 고유 ID",
                    required = true,
                    example = "DEFECT-2024-001",
                    schema = @Schema(pattern = "DEFECT-[0-9]{4}-[0-9]{3}")
            )
            @PathVariable String defectId
    ) {
        Page<DefectLogListDto> defectLogList = defectLogService.defectLogList(pageRequestDto, defectId);
        return ResponseEntity.ok(defectLogList);
    }

}