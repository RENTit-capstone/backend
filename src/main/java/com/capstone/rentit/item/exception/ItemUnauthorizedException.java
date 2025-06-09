package com.capstone.rentit.item.exception;

public class ItemUnauthorizedException extends RuntimeException {
    public ItemUnauthorizedException(String message) {
        super(message);
    }
}
