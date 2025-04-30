package com.capstone.rentit.register.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("잘못된 인증 코드입니다.");
    }
}
