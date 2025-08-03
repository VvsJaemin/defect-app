package com.group.defectapp.dto.defect;

import com.group.defectapp.domain.defect.Defect;
import com.group.defectapp.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "결함 생성/수정 요청 DTO", description = "결함 등록 또는 수정 시 필요한 상세 정보")
public class DefectRequestDto {

    @Schema(description = "결함 ID (수정 시 사용)", example = "DT0000000003")
    private String defectId;

    @Schema(description = "프로젝트 ID", example = "PROJ001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String projectId;

    @Schema(description = "담당자 ID", example = "USER001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assigneeId;

    @Schema(
            description = """
        결함 상태 코드  
        - DS1000: 결함등록  
        - DS2000: 결함할당  
        - DS3000: 결함조치 완료  
        - DS3005: To-Do처리  
        - DS3006: To-Do(조치대기)  
        - DS4000: 결함조치 보류(결함아님)  
        - DS4001: 결함조치 반려(조치안됨)  
        - DS4002: 결함 재발생  
        - DS5000: 결함종료  
        - DS6000: 결함해제  
        - DS7000: 결함할당(담당자 이관)
        """,
            example = "DS1000",
            allowableValues = {
                    "DS1000", "DS2000", "DS3000", "DS3005", "DS3006",
                    "DS4000", "DS4001", "DS4002", "DS5000", "DS6000", "DS7000"
            },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String statusCode;


    @Schema(
            description = """
        결함 심각도 코드  
        - 1: 영향없음  
        - 2: 낮음  
        - 3: 보통  
        - 4: 높음  
        - 5: 치명적
        """,
            example = "3",
            allowableValues = { "1", "2", "3", "4", "5" },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String seriousCode;


    @Schema(
            description = """
        결함 처리 순서 코드  
        - IMPROVING: 개선권고  
        - MOMETLY: 즉시해결  
        - STANBY: 대기  
        - WARNING: 주의요망
        """,
            example = "IMPROVING",
            allowableValues = {
                    "IMPROVING", "MOMETLY", "STANBY", "WARNING"
            },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String orderCode;

    @Schema(
            description = """
        결함 유형 코드  
        - DOCUMENT: 문서결함  
        - FUNCTION: 기능결함  
        - IMPROVING: 개선권고  
        - NEW: 신규요청  
        - SYSTEM: 시스템결함  
        - UI: UI결함
        """,
            example = "FUNCTION",
            allowableValues = { "DOCUMENT", "FUNCTION", "IMPROVING", "NEW", "SYSTEM", "UI" },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String defectDivCode;


    @Schema(description = "결함 제목", example = "버튼 클릭 시 오류 발생", requiredMode = Schema.RequiredMode.REQUIRED)
    private String defectTitle;

    @Schema(description = "결함 메뉴 제목", example = "메인 메뉴 > 환경설정", requiredMode = Schema.RequiredMode.REQUIRED)
    private String defectMenuTitle;

    @Schema(description = "결함 URL 정보", example = "https://qms.example.com/defect/1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String defectUrlInfo;

    @Schema(description = "결함 내용", example = "환경설정 저장 버튼을 클릭하면 500 에러 발생", requiredMode = Schema.RequiredMode.REQUIRED)
    private String defectContent;

    @Schema(description = "기타 내용", example = "재현 조건: 관리자 계정 로그인 필수")
    private String defectEtcContent;

    @Schema(description = "공개 여부(Y: 공개, N: 비공개)", example = "Y", allowableValues = {"Y", "N"})
    private String openYn;

    @Schema(description = "로그 시퀀스 목록", example = "[\"LOG001\", \"LOG002\"]")
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