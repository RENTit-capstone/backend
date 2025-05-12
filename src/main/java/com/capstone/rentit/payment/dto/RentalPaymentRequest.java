package com.capstone.rentit.payment.dto;

public record RentalPaymentRequest(Long renterId, Long ownerId, long rentalFee) {}

