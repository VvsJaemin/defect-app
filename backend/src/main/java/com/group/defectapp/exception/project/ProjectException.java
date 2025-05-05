package com.group.defectapp.exception.project;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ProjectException extends RuntimeException {

    private String message;
    private int code;

    public ProjectException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
