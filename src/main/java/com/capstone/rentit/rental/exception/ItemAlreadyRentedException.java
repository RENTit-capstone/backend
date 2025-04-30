package com.capstone.rentit.rental.exception;

public class ItemAlreadyRentedException extends RuntimeException {
    public ItemAlreadyRentedException(String message) {
        super(message);
    }
}
