package com.group.defectapp.exception.defect;

import lombok.Getter;

@Getter
public enum DefectCode {

    DEFECT_NOT_FOUND("결함을 찾을 수 없습니다", 404),
    DEFECT_NOT_REGISTERED("결함 등록에 실패했습니다", 400),
    DEFECT_NOT_MODIFIED("결함 수정에 실패했습니다", 400),
    DEFECT_NOT_REMOVED("결함 삭제에 실패했습니다", 400),
    DEFECT_NOT_FETCHED("결함 목록 조회에 실패했습니다", 500),
    DEFECT_NO_IMAGE("결함 사진이 없습니다.", 400),
    DEFECT_WRITER_ERROR("해당 사용자를 찾을 수 없습니다.", 403),
    DEFECT_STATUS_CODE_ERROR("해당 상태 코드를 찾을 수 없습니다.", 404),
    DEFECT_WRITER_DELETE_ERROR("본인이 등록한 결함만 삭제할 수 있습니다.", 403);

    private final DefectException defectException;

    DefectCode(String message, int code) {
        this.defectException = new DefectException(message, code);
    }

    public DefectException getDefectException() {
        return defectException;
    }
}