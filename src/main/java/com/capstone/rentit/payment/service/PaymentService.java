package com.capstone.rentit.payment.service;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.ExternalPaymentFailedException;
import com.capstone.rentit.payment.exception.PaymentNotLockerException;
import com.capstone.rentit.payment.exception.WalletNotFoundException;
import com.capstone.rentit.payment.nh.*;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final NhBankClient nhBank;

    /* ------------ 1. 현금 ⇆ 포인트 ------------ */

    @Transactional
    public PaymentResponse topUp(TopUpRequest req) {

        Wallet wallet = getOrCreateWallet(req.memberId());
        Payment payment = paymentRepository.save(
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

        Wallet wallet = findWallet(req.memberId());
        Payment payment = paymentRepository.save(
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

        Wallet renter = findWallet(req.renterId());
        Wallet owner  = findWallet(req.ownerId());

        Payment tx = paymentRepository.save(
                Payment.create(PaymentType.RENTAL_FEE, req.renterId(), req.ownerId(), req.rentalFee()));

        renter.withdraw(req.rentalFee());
        owner.deposit(req.rentalFee());

        tx.approve(null); // 내부 이체 — 외부 참조 없음
        return new PaymentResponse(tx.getId(), tx.getStatus());
    }

    /** 사물함 이용료 (LockerPaymentRequest) */
    @Transactional
    public PaymentResponse payLockerFee(LockerPaymentRequest req) {

        assertLockerFee(req);

        Wallet payer = findWallet(req.payerId());

        /* 사물함 운영 계정(가맹점) — 시스템 내 pseudo memberId. 예: 0L */
//        Wallet operator = walletRepository.findForUpdate(0L);

        Payment tx = paymentRepository.save(
                Payment.create(req.lockerFeeType(), req.payerId(), 0L, req.fee()));

        payer.withdraw(req.fee());
//        operator.deposit(req.fee());

        tx.approve(null);
        return new PaymentResponse(tx.getId(), tx.getStatus());
    }

    public void assertCheckBalance(Long memberId, long fee){
        Wallet wallet = findWallet(memberId);
        wallet.checkBalance(fee);
    }

    private final long LOCKER_FEE_BASIC = 1000;
    private final long LOCKER_FEE_PER_HOUR = 500;
    public long getLockerFeeByAction(RentalLockerAction action, Rental rental, LocalDateTime now){
        if(action == RentalLockerAction.PICK_UP_BY_RENTER){
            return calculateLockerFee(rental.getLeftAt(), now);
        }
        else if(action == RentalLockerAction.RETRIEVE_BY_OWNER){
            return calculateLockerFee(rental.getReturnedAt(), now);
        }
        else throw new IllegalArgumentException("부적절한 액션 타입 입니다.");
    }

    public long calculateLockerFee(LocalDateTime start, LocalDateTime end){
        Duration duration = Duration.between(start, end);
        return LOCKER_FEE_BASIC + LOCKER_FEE_PER_HOUR * duration.toHours();
    }

    /* ------------ Util ------------ */

    public Wallet findWallet(Long memberId){
        return walletRepository.findForUpdate(memberId)
                .orElseThrow(() ->
                        new WalletNotFoundException("해당 사용자의 지갑을 찾을 수 없습니다."));
    }

    public Long createWallet(Long memberId){
        return walletRepository.save(
                Wallet.builder().memberId(memberId).balance(0L).build()).getMemberId();
    }

    private Wallet getOrCreateWallet(Long memberId) {
        return walletRepository.findById(memberId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder().memberId(memberId).balance(0L).build()));
    }

    private void assertLockerFee(LockerPaymentRequest req) {
        if (req.lockerFeeType() != PaymentType.LOCKER_FEE_OWNER &&
                req.lockerFeeType() != PaymentType.LOCKER_FEE_RENTER) {
            throw new PaymentNotLockerException("사물함 비용 결제가 아닙니다.");
        }
    }
}
