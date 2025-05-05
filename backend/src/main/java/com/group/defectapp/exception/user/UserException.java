package com.group.defectapp.exception.user;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class UserException extends RuntimeException {

    private String message;
    private int code;

    public UserException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
