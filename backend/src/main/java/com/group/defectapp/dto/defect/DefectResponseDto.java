package com.group.defectapp.dto.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectResponseDto {

    private String defectId;

    private String projectId;

    private String statusCode;

    private String seriousCode;

    private String orderCode;

    private String defectDivCode;

    private String defectTitle;

    private String defectMenuTitle;

    private String defectUrlInfo;

    private String defectContent;

    private String defectEtcContent;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    private String openYn;

    private String assigneeId;

    @Builder.Default
    private List<DefectFileDto> attachmentFiles = new ArrayList<>();

    // 내부 파일 DTO 클래스
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DefectFileDto {

        private String fileId;

        private String logSeq;

        private String logDefectId;

        private String filePath;

        private String fileSeCd;

        private String orgFileName;

        private String sysFileName;

        private Integer idx;

        private LocalDateTime firstRegDtm;

        private String firstRegId;
    }
}