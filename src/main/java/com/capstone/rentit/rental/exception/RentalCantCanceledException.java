package com.capstone.rentit.rental.exception;

public class RentalCantCanceledException extends RuntimeException {
    public RentalCantCanceledException(String message) {
        super(message);
    }
}
