package com.group.defectapp.dto.defectlog;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    private SortedSet<DefectLogFile> defectLogFiles;
}