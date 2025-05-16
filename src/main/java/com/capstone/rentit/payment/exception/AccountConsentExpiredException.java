package com.capstone.rentit.payment.exception;

public class AccountConsentExpiredException extends RuntimeException {
    public AccountConsentExpiredException(String message) {
        super(message);
    }
}
