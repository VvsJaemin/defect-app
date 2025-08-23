package com.group.defectapp.dto.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectRequestDto {

    private String defectId;

    private String projectId;

    private String assigneeId;

    private String statusCode;

    private String seriousCode;

    private String orderCode;

    private String defectDivCode;

    private String defectTitle;

    private String defectMenuTitle;

    private String defectUrlInfo;

    private String defectContent;

    private String defectEtcContent;

    private String openYn;

    private List<String> logSeq;

    public Defect toEntity(User assignee, String newDefectId, String statusCode, String orderCode, String seriousCode) {
        return Defect.builder()
                .defectId(newDefectId)
                .projectId(projectId)
                .assignee(assigneeId)
                .statusCode(statusCode)
                .seriousCode(seriousCode)
                .orderCode(orderCode)
                .defectDivCode(defectDivCode)
                .defectTitle(defectTitle)
                .defectMenuTitle(defectMenuTitle)
                .defectUrlInfo(defectUrlInfo)
                .defectContent(defectContent)
                .defectEtcContent(defectEtcContent)
                .openYn(openYn)
                .createdBy(assignee.getUserId())
                .updatedBy(assignee.getUserId())
                .build();
    }
}