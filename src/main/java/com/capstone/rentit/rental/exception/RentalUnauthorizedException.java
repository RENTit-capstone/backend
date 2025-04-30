package com.capstone.rentit.rental.exception;

public class RentalUnauthorizedException extends RuntimeException {
    public RentalUnauthorizedException(String message) {
        super(message);
    }
}
