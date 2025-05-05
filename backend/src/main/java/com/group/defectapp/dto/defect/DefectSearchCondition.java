package com.group.defectapp.dto.defect;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectSearchCondition {

    private String projectId;
    private String assigneeId;
    private String statusCode;
    private String searchType;
    private String searchText;

}
