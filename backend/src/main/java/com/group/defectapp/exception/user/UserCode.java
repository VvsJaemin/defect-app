package com.group.defectapp.exception.user;

import lombok.Getter;

@Getter
public enum UserCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다", 404),
    USER_NOT_REGISTERED("사용자 등록에 실패했습니다", 400),
    USER_NOT_MODIFIED("사용자 수정에 실패했습니다", 400),
    USER_NOT_REMOVED("사용자 삭제에 실패했습니다", 400),
    USER_ALREADY_EXISTS("이미 존재하는 사용자 아이디입니다.", 400),
    USER_NOT_FETCHED("사용자 목록 조회에 실패했습니다", 500);

    private final UserException userException;

    UserCode(String message, int code) {
        this.userException = new UserException(message, code);
    }

    public UserException getUserException() {
        return userException;
    }
}