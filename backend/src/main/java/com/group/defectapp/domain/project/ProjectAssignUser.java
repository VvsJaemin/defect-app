package com.group.defectapp.domain.project;

import com.group.defectapp.domain.cmCode.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Schema(
        title = "프로젝트 할당 사용자",
        description = "프로젝트에 할당된 사용자 정보를 나타내는 임베디드 엔티티"
)

public class ProjectAssignUser {
    @Schema(
            description = "프로젝트 ID",
            example = "PROJ0001",
            maxLength = 24,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Column(name = "project_id", length = 24)
    private String projectId;

    @Schema(
            description = "할당된 사용자 ID",
            example = "USER0001",
            maxLength = 48,
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    @Column(name = "assign_user_id", length = 48)
    private String userId;
}
