package com.capstone.rentit.payment.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.payment.dto.AccountRegisterRequest;
import com.capstone.rentit.payment.dto.PaymentSearchForm;
import com.capstone.rentit.payment.dto.TopUpRequest;
import com.capstone.rentit.payment.dto.WithdrawRequest;
import com.capstone.rentit.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService walletPaymentService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/wallet")
    public CommonResponse<?> register(@RequestBody @Valid AccountRegisterRequest body) {
        return CommonResponse.success(walletPaymentService.registerAccount(body));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/wallet")
    public CommonResponse<?> getAccount(@Login MemberDto memberDto) {
        return CommonResponse.success(walletPaymentService.getAccount(memberDto.getMemberId()));
    }

    /** 지갑 충전 (현금 → 포인트) */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/wallet/top-up")
    public CommonResponse<?> topUp(
            @RequestBody @Valid TopUpRequest request) {
        return CommonResponse.success(walletPaymentService.topUp(request));
);
    }

    /** 지갑 인출 (포인트 → 현금) */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/wallet/withdraw")
    public CommonResponse<?> withdraw(
            @RequestBody @Valid WithdrawRequest request) {
        return CommonResponse.success(walletPaymentService.withdraw(request));

    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/payments")
    public CommonResponse<?> getPayments(@ModelAttribute("form") PaymentSearchForm form) {
        return CommonResponse.success(walletPaymentService.getPayments(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/payments")
    public CommonResponse<?> getPaymentsForAdmin(@ModelAttribute("form") PaymentSearchForm form) {
        return CommonResponse.success(walletPaymentService.getPayments(form));
    }
}
