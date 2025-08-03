package com.group.defectapp.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "결함 조회용 프로젝트 정보(목록) DTO")
public class DefectProjectListDto {

    @Schema(description = "프로젝트 ID", example = "PROJ0001")
    private String projectId;

    @Schema(description = "프로젝트명", example = "품질관리시스템 v2.0")
    private String projectName;
}