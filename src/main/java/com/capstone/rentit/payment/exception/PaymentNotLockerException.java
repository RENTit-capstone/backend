package com.capstone.rentit.payment.exception;

public class PaymentNotLockerException extends RuntimeException {
    public PaymentNotLockerException(String message) {
        super(message);
    }
}
