package com.capstone.rentit.payment.nh;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NhBankClientStub implements NhBankClient {

    @Override
    public NhTransferResponse withdrawFromUser(Long memberId, long amount, String desc) {
        return new NhTransferResponse(true, "NH-"+System.currentTimeMillis(), "OK");
    }

    @Override
    public NhTransferResponse depositToUser(Long memberId, long amount, String desc) {
        return new NhTransferResponse(true, "NH-"+System.currentTimeMillis(), "OK");
    }
}