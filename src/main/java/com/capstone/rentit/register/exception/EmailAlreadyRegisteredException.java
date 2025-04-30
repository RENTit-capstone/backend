package com.capstone.rentit.register.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("이미 등록된 이메일입니다.");
    }
}
