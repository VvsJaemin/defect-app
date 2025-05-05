package com.group.defectapp.exception.file;


import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FileNotSupportedException extends RuntimeException {

    private String message;
    private int code;

    public FileNotSupportedException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
