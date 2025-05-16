package com.capstone.rentit.payment.service;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.*;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock WalletRepository walletRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock NhApiClient nhClient;

    @InjectMocks PaymentService service;

    static final long MEMBER_A = 10L;
    static final long MEMBER_B = 20L;
    static final long AMOUNT   = 5_000L;

    static Wallet walletOf(long memberId, long balance) {
        return Wallet.builder()
                .memberId(memberId)
                .balance(balance)
                .finAcno("199999999099999999999999")
                .bankCode("011")
                .consentAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();
    }

    @Nested @DisplayName("0. 계좌 등록 및 조회")
    class RegisterAccountTests {

        @Test @DisplayName("계좌 등록 요청 시 Wallet에 계좌 정보가 업데이트된다")
        void registerAccount_shouldUpdateWallet() {
            // given
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            AccountRegisterRequest req =
                    new AccountRegisterRequest(MEMBER_A, "155566677788899900001111", "011");

            // when
            service.registerAccount(req);

            // then
            assertAll(
                    () -> assertThat(w.getFinAcno()).isEqualTo(req.finAcno()),
                    () -> assertThat(w.getBankCode()).isEqualTo("011")
            );
        }

        @Test @DisplayName("Wallet 조회 시 DTO로 매핑되어 반환된다")
        void getAccount_shouldReturnDto() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findAccount(MEMBER_A)).willReturn(Optional.of(w));

            // when
            WalletResponse dto = service.getAccount(MEMBER_A);

            // then
            assertAll(
                    () -> assertThat(dto.finAcno()).isEqualTo(w.getFinAcno()),
                    () -> assertThat(dto.bankCode()).isEqualTo(w.getBankCode())
            );
        }
    }

    @Nested @DisplayName("1. 포인트 충전 (Top-Up)")
    class TopUpTests {

        @Test @DisplayName("충전 성공 시 Wallet 잔액이 증가하고 Payment가 생성된다")
        void topUp_shouldIncreaseBalanceAndCreatePayment() {
            // given
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.drawingTransfer(anyString(), eq(AMOUNT), anyString()))
                    .willReturn(new DrawingTransferResponse(mock(NhHeader.class), w.getFinAcno(), "OK"));

            // when
            service.topUp(new TopUpRequest(MEMBER_A, AMOUNT));

            // then
            assertThat(w.getBalance()).isEqualTo(AMOUNT);
            Payment saved = captureSavedPayment();
            assertAll(
                    () -> assertThat(saved.getType()).isEqualTo(PaymentType.TOP_UP),
                    () -> assertThat(saved.getStatus()).isEqualTo(PaymentStatus.APPROVED)
            );
        }

        @Test @DisplayName("Wallet이 없으면 WalletNotFoundException을 던진다")
        void topUp_shouldThrowWhenWalletNotFound() {
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.empty());

            // then
            assertThatThrownBy(() -> service.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }

        @Test @DisplayName("NH API 실패 시 ExternalPaymentFailedException을 던진다")
        void topUp_shouldThrowWhenExternalFails() {
            // given
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.drawingTransfer(anyString(), anyLong(), anyString()))
                    .willReturn(null);

            // then
            assertThatThrownBy(() -> service.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(ExternalPaymentFailedException.class);

            assertThat(w.getBalance()).isZero();
        }
    }

    @Nested @DisplayName("2. 포인트 인출 (Withdraw)")
    class WithdrawTests {

        @Test @DisplayName("인출 성공 시 Wallet 잔액이 감소하고 Payment가 생성된다")
        void withdraw_shouldDecreaseBalanceAndCreatePayment() {
            // given
            Wallet w = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.deposit(anyString(), eq(AMOUNT), anyString()))
                    .willReturn(new DepositResponse(mock(NhHeader.class), w.getFinAcno(), "OK"));

            // when
            service.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT));

            // then
            assertAll(
                    () -> assertThat(w.getBalance()).isZero(),
                    () -> assertThat(captureSavedPayment().getStatus()).isEqualTo(PaymentStatus.APPROVED)
            );
        }

        @Test @DisplayName("잔액 부족 시 InsufficientBalanceException을 던진다")
        void withdraw_shouldThrowWhenInsufficientBalance() {
            Wallet w = walletOf(MEMBER_A, 100);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));

            assertThatThrownBy(() -> service.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test @DisplayName("Wallet이 없으면 WalletNotFoundException을 던진다")
        void withdraw_shouldThrowWhenWalletNotFound() {
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }

    @Nested @DisplayName("3. 대여비 결제 (Rental Fee)")
    class RentalFeeTests {

        @Test @DisplayName("Rental Fee 결제 시 금액이 올바르게 이동한다")
        void payRentalFee_shouldMoveMoneyBetweenMembers() {
            Wallet renter = walletOf(MEMBER_A, AMOUNT);
            Wallet owner  = walletOf(MEMBER_B, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(walletRepo.findForUpdate(MEMBER_B)).willReturn(Optional.of(owner));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            service.payRentalFee(new RentalPaymentRequest(MEMBER_A, MEMBER_B, AMOUNT));

            // then
            assertAll(
                    () -> assertThat(renter.getBalance()).isZero(),
                    () -> assertThat(owner.getBalance()).isEqualTo(AMOUNT)
            );
        }
    }

    @Nested @DisplayName("4. 사물함 이용료 결제 (Locker Fee)")
    class LockerFeeTests {

        @Test @DisplayName("사물함 요금 납부 시 잔액이 감소한다")
        void payLockerFee_shouldDecreaseBalanceForRenter() {
            Wallet renter = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.payLockerFee(new LockerPaymentRequest(MEMBER_A,
                    PaymentType.LOCKER_FEE_RENTER, AMOUNT));

            assertThat(renter.getBalance()).isZero();
        }

        @Test @DisplayName("잘못된 타입으로 호출 시 PaymentNotLockerException을 던진다")
        void payLockerFee_shouldThrowWhenInvalidType() {
            assertThatThrownBy(() -> service.payLockerFee(
                    new LockerPaymentRequest(MEMBER_A, PaymentType.RENTAL_FEE, AMOUNT)))
                    .isInstanceOf(PaymentNotLockerException.class);
        }
    }

    @Nested @DisplayName("5. 결제내역 조회 (Get Payments)")
    class GetPaymentsTests {

        @Test @DisplayName("검색 조건으로 조회 시 Repository에 위임된다")
        void getPayments_shouldDelegateToRepository() {
            Payment p = Payment.create(PaymentType.TOP_UP, MEMBER_A, null, AMOUNT, null);
            given(paymentRepo.findByCond(any())).willReturn(List.of(p));

            // when
            List<PaymentResponse> list =
                    service.getPayments(new PaymentSearchForm(MEMBER_A, PaymentType.TOP_UP));

            // then
            assertThat(list).hasSize(1)
                    .first()
                    .satisfies(resp -> assertThat(resp.amount()).isEqualTo(AMOUNT));
        }
    }

    // helper to capture saved Payment
    private Payment captureSavedPayment() {
        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepo).should().save(cap.capture());
        return cap.getValue();
    }
}