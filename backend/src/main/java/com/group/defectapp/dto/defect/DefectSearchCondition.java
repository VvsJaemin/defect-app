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

    private String defectId;
    private String defectTitle;
    private String projectId;
    private String assigneeId;
    private String statusCode;
    private String seriousCode;
    private String orderCode;
    private String defectDivCode;
    private String defectUrlInfo;
    private String openYn;
    private String type;

}
