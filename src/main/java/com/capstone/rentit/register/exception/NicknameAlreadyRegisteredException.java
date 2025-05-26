package com.capstone.rentit.register.exception;

public class NicknameAlreadyRegisteredException extends RuntimeException {
    public NicknameAlreadyRegisteredException() {
        super("이미 등록된 닉네임입니다.");
    }
}
