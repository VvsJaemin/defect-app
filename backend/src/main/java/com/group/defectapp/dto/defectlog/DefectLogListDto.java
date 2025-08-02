package com.group.defectapp.dto.defectlog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group.defectapp.domain.defect.DefectFile;
import com.group.defectapp.domain.defectlog.DefectLogFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.SortedSet;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        title = "결함 로그 목록 DTO",
        description = "결함 로그 목록 조회를 위한 데이터 모델 (확장된 정보 포함)"
)
public class DefectLogListDto {

    @Schema(
            description = "로그 순번 (Primary Key)",
            example = "1"
    )
    private Integer logSeq;

    @Schema(
            description = "결함 ID",
            example = "DEFECT-2024-001"
    )
    private String defectId;

    @Schema(
            description = "로그 제목",
            example = "결함 수정 완료"
    )
    private String logTitle;

    @Schema(
            description = "로그 내용",
            example = "결함 수정이 완료되었습니다. 테스트 진행 예정입니다."
    )
    private String logCt;

    @Schema(
            description = "상태 코드",
            example = "DS1000"
    )
    private String statusCd;

    @Schema(
            description = "생성 시간 (yyyy-MM-dd HH:mm:ss)",
            example = "2024-08-01 14:30:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(
            description = "상태 코드명",
            example = "완료"
    )
    private String statusCode;

    @Schema(
            description = "작성자 ID",
            example = "user01"
    )
    private String createdBy;

    @Schema(
            description = "담당자 ID",
            example = "user02"
    )
    private String assignUserId;

    @Schema(
            description = "담당자 이름",
            example = "홍길동"
    )
    private String assignUserName;

    @Schema(
            description = "결함 URL 정보",
            example = "http://example.com/defect/001"
    )
    private String defectUrlInfo;

    @Schema(
            description = "결함 메뉴 제목",
            example = "주문 관리"
    )
    private String defectMenuTitle;

    @Schema(
            description = "결함 제목",
            example = "주문 처리 오류"
    )
    private String defectTitle;

    @Schema(
            description = "고객사명",
            example = "ABC 회사"
    )
    private String customerName;

    @Schema(
            description = "주문 코드",
            example = "ORD-2024-001"
    )
    private String orderCode;

    @Schema(
            description = "심각도 코드",
            example = "HIGH"
    )
    private String seriousCode;

    @Schema(
            description = "결함 구분 코드",
            example = "BUG"
    )
    private String defectDivCode;

    @Schema(
            description = "시스템 파일명",
            example = "sys_file_20240801_001.jpg"
    )
    private String sysFileName;

    @Schema(
            description = "결함 로그 첨부파일 목록 (최대 3개)",
            example = "[{\"idx\": 0, \"org_file_name\": \"screenshot.png\", \"sys_file_name\": \"sys_001.png\"}]"
    )
    private SortedSet<DefectLogFile> defectLogFiles;
}