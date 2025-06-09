package com.capstone.rentit.notification.dto;

import jakarta.validation.constraints.NotEmpty;

public record TokenRequest(@NotEmpty String token) {}
