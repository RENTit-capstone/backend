package com.capstone.rentit.payment.nh;

public record NhTransferResponse(boolean success, String txId, String message) { }

