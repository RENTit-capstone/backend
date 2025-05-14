package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.type.PaymentStatus;

public record PaymentResponse(Long paymentId, PaymentStatus status) {}

