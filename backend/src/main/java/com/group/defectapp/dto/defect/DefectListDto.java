package com.group.defectapp.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "결함 목록 DTO")
public class DefectListDto {

    @Schema(description = "결함 ID", example = "DT0000000001")
    private String defectId;

    @Schema(description = "프로젝트 ID", example = "PROJ0001")
    private String projectId;

    @Schema(description = "프로젝트명", example = "품질관리시스템 v2.0")
    private String projectName;

    @Schema(description = "결함 상태 코드", example = "OPEN")
    private String statusCode;

    @Schema(description = "심각도 코드", example = "HIGH")
    private String seriousCode;

    @Schema(description = "우선순위 코드", example = "P1")
    private String orderCode;

    @Schema(description = "결함 구분 코드", example = "BUG")
    private String defectDivCode;

    @Schema(description = "결함 제목", example = "로그인 오류")
    private String defectTitle;

    @Schema(description = "결함 발생 메뉴명", example = "사용자 로그인")
    private String defectMenuTitle;

    @Schema(description = "결함 URL 정보", example = "https://qms.example.com/auth/login")
    private String defectUrlInfo;

    @Schema(description = "공개 여부(Y/N)", example = "Y")
    private String openYn;

    @Schema(description = "결함 등록 일시", example = "2024-08-03T11:11:22")
    private LocalDateTime createdAt;

    @Schema(description = "등록자 ID", example = "user01")
    private String createdBy;

    @Schema(description = "결함 수정 일시", example = "2024-08-04T09:20:36")
    private LocalDateTime updatedAt;

    @Schema(description = "수정자 ID", example = "admin01")
    private String updatedBy;

    @Schema(description = "담당자 이름", example = "홍길동")
    private String assigneeName;

    @Schema(description = "담당자 ID", example = "USER010")
    private String assigneeId;

    @Schema(description = "첨부(이미지) 개수", example = "2")
    private int imageCount;
}