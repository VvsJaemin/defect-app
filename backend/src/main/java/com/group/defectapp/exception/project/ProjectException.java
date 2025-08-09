package com.group.defectapp.exception.project;

import lombok.Getter;
import lombok.ToString;

@Getter
public class ProjectException extends RuntimeException {

    private final String message;
    private final int code;

    public ProjectException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
