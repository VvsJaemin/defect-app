package com.group.defectapp.exception.file;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
public enum FileCode {

    FILE_NOT_FOUND("유효하지 않은 파일명입니다.", HttpStatus.BAD_REQUEST),
    FILE_FORMAT_NOT_SUPPORTED("지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    UPLOAD_FILE_NOT_SUPPORTED("업로드할 파일이 없습니다.", HttpStatus.NOT_FOUND),
    UPLOAD_FILE_SIZE_LARGE("업로드 파일 크기가 제한을 초과했습니다.", HttpStatus.PAYLOAD_TOO_LARGE);

    private final String message;
    private final HttpStatus httpStatus;

    FileCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public FileNotSupportedException getFileNotSupportedException() {
        return new FileNotSupportedException(this.message, this.httpStatus.value());
    }
}