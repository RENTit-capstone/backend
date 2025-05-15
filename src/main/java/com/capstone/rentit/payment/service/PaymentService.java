package com.capstone.rentit.payment.service;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.ExternalPaymentFailedException;
import com.capstone.rentit.payment.exception.PaymentNotLockerException;
import com.capstone.rentit.payment.exception.WalletNotFoundException;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final NhApiClient nhClient;

    /* ------------ 1. 현금 ⇆ 포인트 ------------ */
    public Long topUp(TopUpRequest request) {

        Wallet wallet = findWallet(request.memberId());
        Payment payment = paymentRepository.save(
                Payment.create(PaymentType.TOP_UP, request.memberId(), null, request.amount()));

        DrawingTransferResponse res = nhClient.drawingTransfer(request.pinAccount(), request.amount(), "RENTit 충전");
        if (res == null) throw new ExternalPaymentFailedException("NH 응답 없음");

        wallet.deposit(request.amount());
        payment.approve(res.Header().IsTuno());
        return payment.getId();
    }

    public Long withdraw(WithdrawRequest request) {

        Wallet wallet = findWallet(request.memberId());
        Payment payment = paymentRepository.save(
                Payment.create(PaymentType.WITHDRAWAL, null, request.memberId(), request.amount()));

        wallet.withdraw(request.amount());
        DepositResponse res = nhClient.deposit(request.pinAccount(), request.amount(), "RENTit 출금");
        if (res == null) throw new ExternalPaymentFailedException("NH 응답 없음");

        payment.approve(res.Header().IsTuno());
        return payment.getId();
    }

    /* ------------ 2. 대여 흐름 ------------ */

    /** 대여비 (대여자 → 소유자) */
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
    public PaymentResponse payLockerFee(LockerPaymentRequest req) {

        assertLockerFee(req);

        Wallet payer = findWallet(req.payerId());

        /* 사물함 운영 계정(가맹점) — 시스템 내 pseudo memberId. 예: 0L */
//        Wallet operator = walletRepository.findForUpdate(0L);

        Payment tx = paymentRepository.save(
                Payment.create(req.lockerFeeType(), req.payerId(), null, req.fee()));

        payer.withdraw(req.fee());
//        operator.deposit(req.fee());

        tx.approve(null);
        return new PaymentResponse(tx.getId(), tx.getStatus());
    }

    @Transactional(readOnly = true)
    public void assertCheckBalance(Long memberId, long fee){
        Wallet wallet = findWallet(memberId);
        wallet.checkBalance(fee);
    }

    private final long LOCKER_FEE_BASIC = 1000;
    private final long LOCKER_FEE_PER_HOUR = 500;
    @Transactional(readOnly = true)
    public long getLockerFeeByAction(RentalLockerAction action, Rental rental, LocalDateTime now){
        if(action == RentalLockerAction.PICK_UP_BY_RENTER){
            return calculateLockerFee(rental.getLeftAt(), now);
        }
        else if(action == RentalLockerAction.RETRIEVE_BY_OWNER){
            return calculateLockerFee(rental.getReturnedAt(), now);
        }
        else throw new IllegalArgumentException("부적절한 액션 타입 입니다.");
    }

    @Transactional(readOnly = true)
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

    private void assertLockerFee(LockerPaymentRequest req) {
        if (req.lockerFeeType() != PaymentType.LOCKER_FEE_OWNER &&
                req.lockerFeeType() != PaymentType.LOCKER_FEE_RENTER) {
            throw new PaymentNotLockerException("사물함 비용 결제가 아닙니다.");
        }
    }
}
