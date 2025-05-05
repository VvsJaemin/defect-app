package com.group.defectapp.dto.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectRequestDto {

    private String defectId;           // 결함 ID (수정 시 사용)
    private String projectId;          // 프로젝트 ID
    private String assigneeId;         // 담당자 ID
    private String statusCode;         // 상태 코드
    private String seriousCode;        // 심각도 코드
    private String orderCode;          // 순서 코드
    private String defectDivCode;      // 결함 분류 코드
    private String defectTitle;        // 결함 제목
    private String defectMenuTitle;    // 결함 메뉴 제목
    private String defectUrlInfo;      // 결함 URL 정보
    private String defectContent;      // 결함 내용
    private String defectEtcContent;   // 기타 내용
    private String openYn;             // 공개 여부



    public Defect toEntity(User assignee, String newDefectId, String statusCode, String orderCode, String seriousCode) {
        return Defect.builder()
                .defectId(newDefectId)  // 필요한 경우에만 ID 설정
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