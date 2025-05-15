package com.capstone.rentit.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nhapi")
public record NhApiProperties(
        String baseUrl,
        String iscd,
        String fintechApsNo,
        String accessToken,
        SvcCodes svcCodes
) {
    public record SvcCodes(String drawing, String deposit) {}
}
