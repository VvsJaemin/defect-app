package com.group.defectapp.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchCondition {

    private String projectName;
    private String customerName;
    private String projectState;
    private String urlInfo;
}
