package com.oncall.alert.exception;

public class OpenSearchQueryException extends RuntimeException {
    public OpenSearchQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
