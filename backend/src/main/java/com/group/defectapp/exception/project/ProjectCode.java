package com.group.defectapp.exception.project;

import com.group.defectapp.exception.defect.DefectException;
import lombok.Getter;

@Getter
public enum ProjectCode {

    PROJECT_NOT_FOUND("프로젝트를 찾을 수 없습니다", 404),
    PROJECT_NOT_REGISTERED("프로젝트 등록에 실패했습니다", 400),
    PROJECT_NOT_MODIFIED("프로젝트 수정에 실패했습니다", 400),
    PROJECT_NOT_REMOVED("프로젝트 삭제에 실패했습니다", 400),
    PROJECT_NOT_FETCHED("프로젝트 목록 조회에 실패했습니다", 500),
    PROJECT_NO_IMAGE("프로젝트 사진이 없습니다.", 400),
    PROJECT_WRITER_ERROR("해당 사용자를 찾을 수 없습니다.", 403);

    private final ProjectException projectException;

    ProjectCode(String message, int code) {
        this.projectException = new ProjectException(message, code);
    }

    public ProjectException getProjectException() {
        return projectException;
    }
}