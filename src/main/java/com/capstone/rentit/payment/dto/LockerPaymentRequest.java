package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.type.PaymentType;

public record LockerPaymentRequest(Long payerId, PaymentType lockerFeeType, long fee) {}
