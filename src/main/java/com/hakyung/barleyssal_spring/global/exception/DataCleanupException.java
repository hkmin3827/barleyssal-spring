package com.hakyung.barleyssal_spring.global.exception;

public class DataCleanupException extends RuntimeException {
    public DataCleanupException(String message, Throwable cause) {
        super(message, cause);
    }
}