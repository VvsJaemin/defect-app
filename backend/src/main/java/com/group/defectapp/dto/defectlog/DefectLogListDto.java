package com.group.defectapp.dto.defectlog;


import com.group.defectapp.domain.defect.DefectFile;
import com.group.defectapp.domain.defectlog.DefectLogFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.SortedSet;

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
    private String sysFileName;

    // 파일 다운로드를 위한 배열 필드 (최대 3개)
    private SortedSet<DefectLogFile> defectLogFiles;




}