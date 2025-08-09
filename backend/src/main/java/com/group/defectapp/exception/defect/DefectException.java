package com.group.defectapp.exception.defect;

import lombok.Getter;
import lombok.ToString;

@Getter
public class DefectException extends RuntimeException {

    private final String message;
    private final int code;

    public DefectException(String message,int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
