package com.group.defectapp.exception.defectLog;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DefectLogException extends RuntimeException {

    private String message;
    private int code;

    public DefectLogException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
