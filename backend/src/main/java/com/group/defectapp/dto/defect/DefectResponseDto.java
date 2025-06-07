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

    // 담당자 정보
    private String assigneeId;


    // 첨부파일 정보 리스트 추가
    @Builder.Default
    private List<DefectFileDto> attachmentFiles = new ArrayList<>();

    // 내부 파일 DTO 클래스
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DefectFileDto {
        private String fileId;           // logSeq + "_" + idx 형태로 구성
        private String logSeq;          // 로그 시퀀스
        private String logDefectId;      // 로그 결함 ID
        private String filePath;         // 파일 경로
        private String fileSeCd;         // 파일 구분 코드
        private String orgFileName;      // 원본 파일명
        private String sysFileName;      // 시스템 파일명
        private Integer idx;             // 인덱스
        private LocalDateTime firstRegDtm; // 최초 등록 일시
        private String firstRegId;       // 최초 등록자 ID
    }



}