package com.group.defectapp.dto.defect;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectProjectListDto {

    private String projectId;
    private String projectName;
}
