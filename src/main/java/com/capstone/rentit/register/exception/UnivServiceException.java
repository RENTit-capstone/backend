package com.capstone.rentit.register.exception;

public class UnivServiceException extends RuntimeException {
    public UnivServiceException(String message) {
        super(message);
    }
    public UnivServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
