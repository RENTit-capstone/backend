package com.capstone.rentit.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NhApiProperties {

    public record SvcCodes(String drawing, String deposit) {}

    @Value("${nhapi.base-url}")        private String baseUrl;
    @Value("${nhapi.iscd}")            private String iscd;
    @Value("${nhapi.fintech-aps-no}")  private String fintechApsNo;
    @Value("${nhapi.access-token}")    private String accessToken;
    @Value("${nhapi.svc-codes.drawing}") private String drawing;
    @Value("${nhapi.svc-codes.deposit}") private String deposit;

    public SvcCodes getSvcCodes() {
        return new SvcCodes(drawing, deposit);
    }
}
