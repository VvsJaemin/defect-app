package com.group.defectapp.exception.user;

import lombok.Getter;
import lombok.ToString;

@Getter
public class UserException extends RuntimeException {

    private final String message;
    private final int code;

    public UserException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }
}
