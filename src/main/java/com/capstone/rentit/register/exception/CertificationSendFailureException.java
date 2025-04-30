package com.capstone.rentit.register.exception;

public class CertificationSendFailureException extends RuntimeException {
    public CertificationSendFailureException() {
        super("인증 코드 발송에 실패했습니다.");
    }
}