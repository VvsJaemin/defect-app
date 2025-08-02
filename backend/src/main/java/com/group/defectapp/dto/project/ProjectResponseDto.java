package com.group.defectapp.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(
        title = "프로젝트 응답 DTO",
        description = "프로젝트 조회 시 반환되는 상세 정보"
)
public class ProjectResponseDto {

    @Schema(
            description = "프로젝트 고유 식별자",
            example = "PROJ001"
    )
    private String projectId;

    @Schema(
            description = "프로젝트명",
            example = "품질관리시스템 v2.0"
    )
    private String projectName;

    @Schema(
            description = "프로젝트 URL 정보",
            example = "https://qms.example.com"
    )
    private String urlInfo;

    @Schema(
            description = "고객사명",
            example = "삼성전자"
    )
    private String customerName;

    @Schema(
            description = "프로젝트 상태 코드",
            example = "DEV",
            allowableValues = {"DEV", "OPERATE", "TEST"}
    )
    private String statusCode;

    @Schema(
            description = "기타 정보",
            example = "프로젝트 추가 설명 및 참고사항"
    )
    private String etcInfo;

    @Schema(
            description = "사용 여부",
            example = "Y",
            allowableValues = {"Y", "N"}
    )
    private String useYn;

    @Schema(
            description = "생성자 ID",
            example = "ADMIN001"
    )
    private String createdBy;

    @Schema(
            description = "수정자 ID",
            example = "USER002"
    )
    private String updatedBy;

    @Schema(
            description = "생성일시",
            example = "2024-01-15 09:30:00",
            type = "string",
            format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(
            description = "수정일시",
            example = "2024-01-16 14:20:00",
            type = "string",
            format = "date-time"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(
            description = "할당된 사용자 ID 목록",
            example = "[\"USER001\", \"USER002\", \"USER003\"]"
    )
    private Set<String> assignedUsers;

    @Schema(
            description = "할당된 사용자 ID와 이름의 매핑",
            example = "{\"USER001\": \"김철수\", \"USER002\": \"이영희\", \"USER003\": \"박민수\"}"
    )
    private Map<String, String> assignedUsersMap;

    @Schema(
            description = "할당된 사용자 수",
            example = "3",
            minimum = "0"
    )
    private int assignedUserCnt;
}