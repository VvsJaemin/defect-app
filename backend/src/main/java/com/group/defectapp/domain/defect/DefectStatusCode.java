package com.group.defectapp.domain.defect;

public enum DefectStatusCode {
    REGISTERED("DS1000", "결함등록"),
    ASSIGNED("DS2000", "결함할당"),
    COMPLETED("DS3000", "결함조치 완료"),
    TODO_PROCESSED("DS3005", "To-Do처리"),
    TODO_WAITING("DS3006", "To-Do(조치대기)"),
    HOLD_NOT_DEFECT("DS4000", "결함조치 보류(결함아님)"),
    REJECTED_NOT_FIXED("DS4001", "결함조치 반려(조치안됨)"),
    REOCCURRED("DS4002", "결함 재발생"),
    CLOSED("DS5000", "결함종료"),
    CANCELED("DS6000", "결함해제"),
    ASSIGNED_TRANSFER("DS7000", "결함할당(담당자 이관)");

    private final String code;
    private final String description;

    DefectStatusCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}