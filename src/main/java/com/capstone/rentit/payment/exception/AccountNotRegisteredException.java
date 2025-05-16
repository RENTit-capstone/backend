package com.capstone.rentit.payment.exception;

public class AccountNotRegisteredException extends RuntimeException {
    public AccountNotRegisteredException(String message) {
        super(message);
    }
}
