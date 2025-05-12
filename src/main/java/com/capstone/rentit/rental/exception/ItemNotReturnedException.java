package com.capstone.rentit.rental.exception;

public class ItemNotReturnedException extends RuntimeException {
    public ItemNotReturnedException(String message) {
        super(message);
    }
}
