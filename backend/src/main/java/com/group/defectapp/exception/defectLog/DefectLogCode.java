package com.group.defectapp.exception.defectLog;

import com.group.defectapp.exception.defect.DefectException;
import lombok.Getter;

@Getter
public enum DefectLogCode {

    DEFECT_NOT_FOUND("결함 이력을 찾을 수 없습니다", 404),
    DEFECT_NOT_REGISTERED("결함 이력 등록에 실패했습니다", 400);

    private final DefectException defectException;

    DefectLogCode(String message, int code) {
        this.defectException = new DefectException(message, code);
    }

}