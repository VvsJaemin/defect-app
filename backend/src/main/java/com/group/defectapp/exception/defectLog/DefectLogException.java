package com.group.defectapp.exception.defectLog;

import lombok.Getter;
import lombok.ToString;

@Getter
public class DefectLogException extends RuntimeException {

    private final String message;
    private final int code;

    public DefectLogException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
