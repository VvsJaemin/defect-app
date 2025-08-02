package com.group.defectapp.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "프로젝트 검색 조건")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchCondition {

    @Schema(description = "프로젝트명", example = "품질관리시스템")
    private String projectName;

    @Schema(description = "고객명", example = "삼성전자")
    private String customerName;

    @Schema(description = "프로젝트 상태", example = "진행중", allowableValues = {"대기", "진행중", "완료", "중단"})
    private String projectState;

    @Schema(description = "URL 정보", example = "https://project.example.com")
    private String urlInfo;

    // 테스트에서 사용하는 필드들 추가
    @Schema(description = "검색 타입", example = "projectName", allowableValues = {"projectName", "customerName", "projectState"})
    private String searchType;

    @Schema(description = "검색 텍스트", example = "품질관리")
    private String searchText;
}