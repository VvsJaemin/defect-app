
package com.group.defectapp.dto.project;

import com.group.defectapp.domain.cmCode.CommonCode;
import com.group.defectapp.domain.project.Project;
import com.group.defectapp.domain.project.ProjectAssignUser;
import com.group.defectapp.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "프로젝트 요청 DTO",
        description = "프로젝트 생성 및 수정을 위한 요청 데이터"
)
public class ProjectRequestDto {

    @Schema(
            description = "프로젝트 고유 식별자",
            example = "PROJ001",
            nullable = true
    )
    private String projectId;

    @Schema(
            description = "프로젝트명",
            example = "품질관리시스템 v2.0",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100
    )
    private String projectName;

    @Schema(
            description = "프로젝트 URL 정보",
            example = "https://qms.example.com",
            nullable = true
    )
    private String urlInfo;

    @Schema(
            description = "고객사명",
            example = "삼성전자",
            nullable = true,
            maxLength = 50
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
            example = "프로젝트 추가 설명",
            nullable = true
    )
    private String etcInfo;

    @Schema(
            description = "사용 여부",
            example = "Y",
            defaultValue = "Y",
            allowableValues = {"Y", "N"}
    )
    private String useYn;

    @Schema(
            description = "담당자 ID",
            example = "USER001",
            required = true
    )
    private String assigneeId;

    @Schema(
            description = "할당된 사용자 ID 목록",
            example = "[\"USER001\", \"USER002\", \"USER003\"]",
            nullable = true
    )
    private Set<String> projAssignedUsers;

    @Schema(
            description = "생성자 ID",
            example = "ADMIN001",
            nullable = true
    )
    private String createdBy;

    @Schema(
            description = "수정자 ID",
            example = "USER002",
            nullable = true
    )
    private String updatedBy;

    public Project toEntity(User assignee, String newProjectId, CommonCode commonCode) {
        return Project.builder()
                .projectId(newProjectId)
                .projectName(projectName)
                .urlInfo(urlInfo)
                .customerName(customerName)
                .statusCode(commonCode.getSeCode())
                .etcInfo(etcInfo)
                .useYn(useYn != null ? useYn : "Y") // 기본값 설정
                .assignee(assignee)
                .createdBy(createdBy != null ? createdBy : (assignee != null ? assignee.getUserId() : null))
                .updatedBy(createdBy != null ? createdBy : (assignee != null ? assignee.getUserId() : null))
                .projAssignedUsers(projAssignedUsers)
                .build();
    }
}