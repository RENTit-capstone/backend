package com.capstone.rentit.payment.service;

import com.capstone.rentit.payment.config.NhApiProperties;
import com.capstone.rentit.payment.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class NhApiClient {

    private final RestTemplate restTemplate;
    private final NhApiProperties prop;

    private <T, R> R post(String path, T body, Class<R> resType) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<T> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                prop.getBaseUrl() + path,
                entity,
                resType
        );
    }

    /* 지갑 충전 : DrawingTransfer */
    public DrawingTransferResponse drawingTransfer(String pinAccount, long amount, String memo) {

        DrawingTransferRequest req = new DrawingTransferRequest(
                NhHeader.create("DrawingTransfer",
                        prop.getSvcCodes().drawing(),
                        prop),
                pinAccount,
                String.valueOf(amount),
                memo,
                ""
        );

        return post("/DrawingTransfer.nh", req, DrawingTransferResponse.class);
    }

    /* 지갑 출금 : ReceivedTransferAccountNumber */
    public DepositResponse deposit(String pinAccount, long amount, String memo) {

        DepositRequest req = new DepositRequest(
                NhHeader.create("ReceivedTransferAccountNumber",
                        prop.getSvcCodes().deposit(),
                        prop),
                pinAccount,
                String.valueOf(amount),
                memo
        );

        return post("/ReceivedTransferAccountNumber.nh", req, DepositResponse.class);
    }
}