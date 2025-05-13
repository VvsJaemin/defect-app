package com.group.defectapp.exception.common;

public class HttpMessageException extends RuntimeException {

    private final HttpMessageCode httpMessageCode;

    public HttpMessageException(HttpMessageCode httpMessageCode) {
        super(httpMessageCode.getMessage());
        this.httpMessageCode = httpMessageCode;
    }

    public int getCode() {
        return httpMessageCode.getCode();
    }

    public String getErrorMessage() {
        return httpMessageCode.getMessage();
    }
}
