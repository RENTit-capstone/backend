package com.capstone.rentit.payment.service;

import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.*;
import com.capstone.rentit.payment.nh.*;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock  WalletRepository walletRepo;
    @Mock  PaymentRepository paymentRepo;
    @Mock  NhBankClient nhBank;
    @InjectMocks PaymentService paymentService;

    private static final long MEMBER_A = 1L;
    private static final long MEMBER_B = 2L;
    private static final long AMOUNT   = 10_000L;

    private static Wallet walletOf(long memberId, long balance) {
        return Wallet.builder().memberId(memberId).balance(balance).build();
    }

    @Nested class TopUpTests {

        @Test void topUp_success_deposits_existingWallet() {
            // given
            Wallet wallet = walletOf(MEMBER_A, 0);
            Payment saved = Payment.create(PaymentType.TOP_UP, MEMBER_A, null, AMOUNT);
            given(walletRepo.findById(MEMBER_A)).willReturn(Optional.of(wallet));
            given(paymentRepo.save(any(Payment.class))).willReturn(saved);
            given(nhBank.withdrawFromUser(eq(MEMBER_A), eq(AMOUNT), anyString()))
                    .willReturn(new NhTransferResponse(true, "TX1", "OK"));

            // when
            PaymentResponse res =
                    paymentService.topUp(new TopUpRequest(MEMBER_A, AMOUNT));

            // then
            assertThat(wallet.getBalance()).isEqualTo(AMOUNT);
            assertThat(res.status()).isEqualTo(PaymentStatus.APPROVED);
            then(paymentRepo).should().save(any(Payment.class));
        }

        @Test void topUp_createsWallet_ifNotExists() {
            // given
            given(walletRepo.findById(MEMBER_A)).willReturn(Optional.empty());
            given(walletRepo.save(any(Wallet.class)))
                    .willAnswer(inv -> inv.getArgument(0));   // return 새 Wallet
            given(paymentRepo.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(nhBank.withdrawFromUser(anyLong(), anyLong(), anyString()))
                    .willReturn(new NhTransferResponse(true, "TX2", "OK"));

            // when
            paymentService.topUp(new TopUpRequest(MEMBER_A, AMOUNT));

            // then
            then(walletRepo).should().save(any(Wallet.class));
        }

        @Test void topUp_externalFail_throws_and_noDeposit() {
            // given
            Wallet wallet = walletOf(MEMBER_A, 0);
            given(walletRepo.findById(MEMBER_A)).willReturn(Optional.of(wallet));
            given(paymentRepo.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(nhBank.withdrawFromUser(anyLong(), anyLong(), anyString()))
                    .willReturn(new NhTransferResponse(false, null, "ERR"));

            // when / then
            assertThatThrownBy(() ->
                    paymentService.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(ExternalPaymentFailedException.class);

            assertThat(wallet.getBalance()).isZero();
        }
    }

    @Nested class WithdrawTests {

        @Test void withdraw_success() {
            // given
            Wallet wallet = walletOf(MEMBER_A, AMOUNT);
            Payment saved = Payment.create(PaymentType.WITHDRAWAL, null, MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));
            given(paymentRepo.save(any(Payment.class))).willReturn(saved);
            given(nhBank.depositToUser(anyLong(), anyLong(), anyString()))
                    .willReturn(new NhTransferResponse(true, "TX3", "OK"));

            // when
            PaymentResponse res =
                    paymentService.withdraw(new WithdrawalRequest(MEMBER_A, AMOUNT));

            // then
            assertThat(wallet.getBalance()).isZero();
            assertThat(res.status()).isEqualTo(PaymentStatus.APPROVED);
        }

        @Test void withdraw_walletNotFound_throws() {
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.empty());
            assertThatThrownBy(() ->
                    paymentService.withdraw(new WithdrawalRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }

        @Test void withdraw_insufficientBalance_throws() {
            Wallet wallet = walletOf(MEMBER_A, 1_000);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));
            assertThatThrownBy(() ->
                    paymentService.withdraw(new WithdrawalRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    @Nested class RentalFeeTests {

        @Test void payRentalFee_movesMoney_betweenMembers() {
            // given
            Wallet renter = walletOf(MEMBER_A, AMOUNT);
            Wallet owner  = walletOf(MEMBER_B, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(walletRepo.findForUpdate(MEMBER_B)).willReturn(Optional.of(owner));
            given(paymentRepo.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            paymentService.payRentalFee(
                    new RentalPaymentRequest(MEMBER_A, MEMBER_B, AMOUNT));

            // then
            assertThat(renter.getBalance()).isZero();
            assertThat(owner.getBalance()).isEqualTo(AMOUNT);
        }
    }

    @Nested class LockerFeeTests {

        @Test void payLockerFee_renter_success() {
            // given
            Wallet renter = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(paymentRepo.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            paymentService.payLockerFee(
                    new LockerPaymentRequest(MEMBER_A,
                            PaymentType.LOCKER_FEE_RENTER, AMOUNT));

            // then
            assertThat(renter.getBalance()).isZero();
        }

        @Test void payLockerFee_invalidType_throws() {
            assertThatThrownBy(() ->
                    paymentService.payLockerFee(
                            new LockerPaymentRequest(MEMBER_A,
                                    PaymentType.RENTAL_FEE, AMOUNT)))
                    .isInstanceOf(PaymentNotLockerException.class);
        }
    }
}
