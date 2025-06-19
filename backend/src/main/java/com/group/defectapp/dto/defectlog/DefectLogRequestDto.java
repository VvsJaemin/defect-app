package com.group.defectapp.dto.defectlog;

import com.group.defectapp.domain.defectlog.DefectLog;
import com.group.defectapp.domain.project.Project;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLogRequestDto {
    private String defectId;
    private String logTitle;
    private String logCt;
    private String statusCd;
    private String createdBy;
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