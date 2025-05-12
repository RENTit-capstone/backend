package com.capstone.rentit.payment.service;

import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.ExternalPaymentFailedException;
import com.capstone.rentit.payment.nh.*;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WalletRepository walletRepo;
    private final PaymentRepository payRepo;
    private final NhBankClient nhBank;

    /* ------------ 1. 현금 ⇆ 포인트 ------------ */

    @Transactional
    public PaymentResponse topUp(TopUpRequest req) {

        Wallet wallet = getOrCreateWallet(req.memberId());
        Payment payment = payRepo.save(
                Payment.create(PaymentType.TOP_UP, req.memberId(), null, req.amount()));

        NhTransferResponse nh = nhBank.withdrawFromUser(req.memberId(), req.amount(),
                "[RENTit] 포인트 충전");

        if (!nh.success()) throw new ExternalPaymentFailedException(nh.message());

        wallet.deposit(req.amount());
        payment.approve(nh.txId());

        return new PaymentResponse(payment.getId(), payment.getStatus());
    }

    @Transactional
    public PaymentResponse withdraw(WithdrawalRequest req) {

        Wallet wallet = walletRepo.findForUpdate(req.memberId());
        Payment payment = payRepo.save(
                Payment.create(PaymentType.WITHDRAWAL, null, req.memberId(), req.amount()));

        wallet.withdraw(req.amount());

        NhTransferResponse nh = nhBank.depositToUser(req.memberId(), req.amount(),
                "[RENTit] 포인트 출금");

        if (!nh.success()) throw new ExternalPaymentFailedException(nh.message());

        payment.approve(nh.txId());

        return new PaymentResponse(payment.getId(), payment.getStatus());
    }

    /* ------------ 2. 대여 흐름 ------------ */

    /** 대여비 (대여자 → 소유자) */
    @Transactional
    public PaymentResponse payRentalFee(RentalPaymentRequest req) {

        Wallet renter = walletRepo.findForUpdate(req.renterId());
        Wallet owner  = walletRepo.findForUpdate(req.ownerId());

        Payment tx = payRepo.save(
                Payment.create(PaymentType.RENTAL_FEE, req.renterId(), req.ownerId(), req.rentalFee()));

        renter.withdraw(req.rentalFee());
        owner.deposit(req.rentalFee());

        tx.approve(null); // 내부 이체 — 외부 참조 없음
        return new PaymentResponse(tx.getId(), tx.getStatus());
    }

    /** 사물함 이용료 (LockerPaymentRequest) */
    @Transactional
    public PaymentResponse payLockerFee(LockerPaymentRequest req) {

        if (req.lockerFeeType() != PaymentType.LOCKER_FEE_OWNER &&
                req.lockerFeeType() != PaymentType.LOCKER_FEE_RENTER) {
            throw new IllegalArgumentException("lockerFeeType must be *_LOCKER_*");
        }

        Wallet payer = walletRepo.findForUpdate(req.payerId());

        /* 사물함 운영 계정(가맹점) — 시스템 내 pseudo memberId. 예: 0L */
        Wallet operator = walletRepo.findForUpdate(0L);

        Payment tx = payRepo.save(
                Payment.create(req.lockerFeeType(), req.payerId(), 0L, req.fee()));

        payer.withdraw(req.fee());
        operator.deposit(req.fee());

        tx.approve(null);
        return new PaymentResponse(tx.getId(), tx.getStatus());
    }

    /* ------------ Util ------------ */

    private Wallet getOrCreateWallet(Long memberId) {
        return walletRepo.findById(memberId)
                .orElseGet(() -> walletRepo.save(
                        Wallet.builder().memberId(memberId).balance(0L).build()));
    }
}
