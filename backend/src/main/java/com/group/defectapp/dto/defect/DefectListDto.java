package com.group.defectapp.dto.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DefectListDto {

    private String defectId;
    private String projectId;
    private String projectName;
    private String statusCode;
    private String seriousCode;
    private String orderCode;
    private String defectDivCode;
    private String defectTitle;
    private String defectMenuTitle;
    private String defectUrlInfo;
    private String openYn;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String assigneeName;
    private String assigneeId;
    private int imageCount;

}