package com.capstone.rentit.otp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class OtpDto {
    private String code;
    private Instant expiresAt;
}