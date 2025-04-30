package com.capstone.rentit.register.exception;

public class UnivNotCertifiedException extends RuntimeException {
    public UnivNotCertifiedException() {
        super("미인증 이메일입니다.");
    }
}
