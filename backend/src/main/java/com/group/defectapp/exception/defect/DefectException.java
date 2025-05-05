package com.group.defectapp.exception.defect;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DefectException extends RuntimeException {

    private String message;
    private int code;

    public DefectException(String message,int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
