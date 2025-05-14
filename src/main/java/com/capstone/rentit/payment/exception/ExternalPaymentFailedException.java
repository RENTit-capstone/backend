package com.capstone.rentit.payment.exception;

public class ExternalPaymentFailedException extends RuntimeException {
    public ExternalPaymentFailedException(String message) {
        super(message);
    }
}
