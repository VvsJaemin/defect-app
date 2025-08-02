package com.group.defectapp.dto.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.project.Project;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        title = "결함 로그 요청 DTO",
        description = "결함 로그 생성 및 수정을 위한 요청 데이터 모델"
)
public class DefectLogRequestDto {

    @Schema(
            description = "결함 ID",
            example = "DEFECT-2024-001",
            required = true
    )
    private String defectId;

    @Schema(
            description = "로그 제목",
            example = "결함 수정 완료",
            maxLength = 1024
    )
    private String logTitle;

    @Schema(
            description = "로그 내용",
            example = "결함 수정이 완료되었습니다. 테스트 진행 예정입니다.",
            maxLength = 4000
    )
    private String logCt;

    @Schema(
            description = "상태 코드",
            example = "DS1000",
            maxLength = 24
    )
    private String statusCd;

    @Schema(
            description = "작성자 ID",
            example = "user01",
            maxLength = 48
    )
    private String createdBy;

    @Schema(
            description = "담당자 ID",
            example = "user02",
            maxLength = 48
    )
    private String assignUserId;

    public DefectLog toEntity() {
        return DefectLog.builder()
                .defectId(defectId)
                .logTitle(logTitle)
                .logCt(logCt)
                .statusCd(statusCd)
                .createdBy(createdBy)
                .build();
    }
}