package com.group.defectapp.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(title = "결함 검색 조건 DTO", description = "결함 목록 검색 및 필터링을 위한 조건")
public class DefectSearchCondition {

    @Schema(description = "결함 ID", example = "DT0000000003")
    private String defectId;

    @Schema(description = "결함 제목", example = "버튼 클릭 시 오류")
    private String defectTitle;

    @Schema(description = "프로젝트 ID", example = "PROJ001")
    private String projectId;

    @Schema(description = "담당자 ID", example = "USER001")
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
            }
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
            allowableValues = { "1", "2", "3", "4", "5" }
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
            }
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
            allowableValues = { "DOCUMENT", "FUNCTION", "IMPROVING", "NEW", "SYSTEM", "UI" }
    )
    private String defectDivCode;

    @Schema(description = "결함 URL 정보", example = "https://qms.example.com/defect/1")
    private String defectUrlInfo;

    @Schema(description = "공개 여부(Y: 공개, N: 비공개)", example = "Y", allowableValues = {"Y", "N"})
    private String openYn;

    @Schema(description = "검색 타입", example = "defectTitle",
            allowableValues = {"defectId", "defectTitle", "projectId", "assigneeId", "statusCode", "seriousCode"})
    private String type;

    // 테스트에서 사용하는 필드들 추가
    @Schema(description = "검색 타입 (테스트용)", example = "defectTitle",
            allowableValues = {"defectTitle", "defectContent", "assigneeId", "statusCode"})
    private String searchType;

    @Schema(description = "검색 텍스트 (테스트용)", example = "버튼 오류")
    private String searchText;
}