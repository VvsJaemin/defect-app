package com.group.defectapp.dto.defectlog;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLogListDto {

    private Integer logSeq;
    private String defectId;
    private String logTitle;
    private String logCt;
    private String statusCd;
    private LocalDateTime createdAt;
    private String statusCode;
    private String createdBy;
    private String assignUserId;
    private String assignUserName;
    private String defectUrlInfo;
    private String defectMenuTitle;
    private String defectTitle;
    private String customerName;
    private String orderCode;
    private String seriousCode;
    private String defectDivCode;


}