package com.group.defectapp.exception.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum HttpMessageCode {

    JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST.value(), "JSON 파싱 오류가 발생했습니다."),
    ARRAY_EXPECTED(HttpStatus.BAD_REQUEST.value(), "배열 형식으로 데이터를 전송해야 합니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST.value(), "요청 형식이 올바르지 않습니다.");

    private final int code;
    private final String message;

    HttpMessageCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public HttpMessageException getHttpException() {
        return new HttpMessageException(this);
    }
}
