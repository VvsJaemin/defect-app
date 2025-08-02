package com.group.defectapp.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "프로젝트 할당 사용자 목록 DTO",
        description = "프로젝트에 할당된 사용자의 기본 정보"
)
public class ProjectUserListDto {

    @Schema(
            description = "사용자 ID",
            example = "USER001",
            required = true
    )
    private String userId;

    @Schema(
            description = "사용자명",
            example = "김철수",
            required = true
    )
    private String userName;

    @Schema(
            description = "사용자 구분 코드",
            example = "CU",
            required = true,
            allowableValues = {"CU", "DM", "DP", "MG", "QA"}
    )
    private String userSeCd;

}
