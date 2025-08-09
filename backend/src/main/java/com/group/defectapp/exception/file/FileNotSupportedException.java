package com.group.defectapp.exception.file;


import lombok.Getter;
import lombok.ToString;

@Getter
public class FileNotSupportedException extends RuntimeException {

    private final String message;
    private final int code;

    public FileNotSupportedException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
