package com.capstone.rentit.payment.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.payment.dto.TopUpRequest;
import com.capstone.rentit.payment.dto.WithdrawRequest;
import com.capstone.rentit.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService walletPaymentService;

    /** 지갑 충전 (현금 → 포인트) */
    @PostMapping("/top-up")
    public CommonResponse<?> topUp(
            @RequestBody @Valid TopUpRequest request) {

        walletPaymentService.topUp(request);
        return CommonResponse.success(null);
    }

    /** 지갑 인출 (포인트 → 현금) */
    @PostMapping("/withdraw")
    public CommonResponse<?> withdraw(
            @RequestBody @Valid WithdrawRequest request) {

        walletPaymentService.withdraw(request);
        return CommonResponse.success(null);

    }
}
