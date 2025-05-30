package com.capstone.rentit.payment.service;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.domain.Wallet;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.*;
import com.capstone.rentit.payment.repository.PaymentRepository;
import com.capstone.rentit.payment.repository.WalletRepository;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock WalletRepository walletRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock RentalRepository rentalRepo;
    @Mock NhApiClient nhClient;

    @InjectMocks PaymentService service;

    static final long MEMBER_A   = 10L;
    static final long MEMBER_B   = 20L;
    static final long AMOUNT     = 5_000L;
    static final long RENTAL_ID  = 42L;

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

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("0. 계좌 등록 및 조회")
    class RegisterAndGetAccountTests {

        @Test @DisplayName("계좌 등록 요청 시 Wallet에 계좌 정보가 업데이트된다")
        void registerAccount_shouldUpdateWallet() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            var req = new AccountRegisterRequest(MEMBER_A, "155566677788899900001111", "011");

            service.registerAccount(req);

            assertAll(
                    () -> assertThat(w.getFinAcno()).isEqualTo("155566677788899900001111"),
                    () -> assertThat(w.getBankCode()).isEqualTo("011")
            );
        }

        @Test @DisplayName("Wallet 조회 시 DTO로 매핑되어 반환된다")
        void getAccount_shouldReturnDto() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findAccount(MEMBER_A)).willReturn(Optional.of(w));

            WalletResponse dto = service.getAccount(MEMBER_A);

            assertAll(
                    () -> assertThat(dto.finAcno()).isEqualTo(w.getFinAcno()),
                    () -> assertThat(dto.bankCode()).isEqualTo(w.getBankCode())
            );
        }

        @Test @DisplayName("Wallet이 없으면 WalletNotFoundException을 던진다")
        void getAccount_shouldThrowWhenNotFound() {
            given(walletRepo.findAccount(MEMBER_A)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAccount(MEMBER_A))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("1. 포인트 충전 (Top-Up)")
    class TopUpTests {

        @Test @DisplayName("충전 성공 시 Wallet 잔액이 증가하고 Payment가 APPROVED 된다")
        void topUp_shouldIncreaseBalanceAndApprovePayment() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.drawingTransfer(anyString(), eq(AMOUNT), anyString()))
                    .willReturn(new DrawingTransferResponse(mock(NhHeader.class), w.getFinAcno(), "OK"));

            service.topUp(new TopUpRequest(MEMBER_A, AMOUNT));

            assertThat(w.getBalance()).isEqualTo(AMOUNT);
            Payment saved = captureSavedPayment();
            assertAll(
                    () -> assertThat(saved.getType()).isEqualTo(PaymentType.TOP_UP),
                    () -> assertThat(saved.getStatus()).isEqualTo(PaymentStatus.APPROVED)
            );
        }

        @Test @DisplayName("NH 응답이 없으면 ExternalPaymentFailedException을 던진다")
        void topUp_shouldThrowWhenExternalFails() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.drawingTransfer(anyString(), anyLong(), anyString()))
                    .willReturn(null);

            assertThatThrownBy(() -> service.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(ExternalPaymentFailedException.class);
            assertThat(w.getBalance()).isZero();
        }

        @Test @DisplayName("Wallet이 없으면 WalletNotFoundException을 던진다")
        void topUp_shouldThrowWhenWalletNotFound() {
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("2. 포인트 인출 (Withdraw)")
    class WithdrawTests {

        @Test @DisplayName("인출 성공 시 Wallet 잔액이 감소하고 Payment가 APPROVED 된다")
        void withdraw_shouldDecreaseBalanceAndApprovePayment() {
            Wallet w = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(nhClient.deposit(anyString(), eq(AMOUNT), anyString()))
                    .willReturn(new DepositResponse(mock(NhHeader.class), w.getFinAcno(), "OK"));

            service.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT));

            assertAll(
                    () -> assertThat(w.getBalance()).isZero(),
                    () -> assertThat(captureSavedPayment().getStatus()).isEqualTo(PaymentStatus.APPROVED)
            );
        }

        @Test @DisplayName("잔액 부족 시 InsufficientBalanceException을 던진다")
        void withdraw_shouldThrowWhenInsufficient() {
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

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("3. 대여비 흐름")
    class RentalFeeFlowTests {

        @Test @DisplayName("요청 시 금액이 출금되고 Payment가 생성된다")
        void requestRentalFee_shouldWithdrawAndSavePayment() {
            Wallet renter = walletOf(MEMBER_A, AMOUNT * 2);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            var req = new RentalPaymentRequest(MEMBER_A, MEMBER_B, AMOUNT);
            service.requestRentalFee(req, RENTAL_ID);

            assertThat(renter.getBalance()).isEqualTo(AMOUNT);
            Payment saved = captureSavedPayment();
            assertAll(
                    () -> assertThat(saved.getType()).isEqualTo(PaymentType.RENTAL_FEE),
                    () -> assertThat(saved.getFromMemberId()).isEqualTo(MEMBER_A),
                    () -> assertThat(saved.getToMemberId()).isEqualTo(MEMBER_B),
                    () -> assertThat(saved.getPaymentRentalId()).isEqualTo(RENTAL_ID)
            );
        }

        @Test @DisplayName("요청된 결제가 없으면 PaymentNotFoundException 을 던진다")
        void payRentalFee_shouldThrowWhenNotFound() {
            given(paymentRepo.findByRentalId(RENTAL_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.payRentalFee(RENTAL_ID))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test @DisplayName("결제 승인 시 금액이 입금되고 Payment가 APPROVED 된다")
        void payRentalFee_shouldDepositAndApprovePayment() {
            Payment p = Payment.create(PaymentType.RENTAL_FEE, MEMBER_A, MEMBER_B, AMOUNT, RENTAL_ID);
            Wallet owner = walletOf(MEMBER_B, 0);

            given(paymentRepo.findByRentalId(RENTAL_ID)).willReturn(Optional.of(p));
            given(walletRepo.findForUpdate(MEMBER_B)).willReturn(Optional.of(owner));

            service.payRentalFee(RENTAL_ID);

            assertAll(
                    () -> assertThat(owner.getBalance()).isEqualTo(AMOUNT),
                    () -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.APPROVED)
            );
        }

        @Test @DisplayName("결제 취소 시 금액이 환불되고 Payment가 CANCELED 된다")
        void cancelPayment_shouldRefundAndCancel() {
            Payment p = Payment.create(PaymentType.RENTAL_FEE, MEMBER_A, MEMBER_B, AMOUNT, RENTAL_ID);
            Wallet renter = walletOf(MEMBER_A, 100);

            given(paymentRepo.findByRentalId(RENTAL_ID)).willReturn(Optional.of(p));
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));

            service.cancelPayment(RENTAL_ID);

            assertAll(
                    () -> assertThat(renter.getBalance()).isEqualTo(100 + AMOUNT),
                    () -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.CANCELED)
            );
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("4. 사물함 이용료 결제 (Locker Fee)")
    class LockerFeeTests {

        @Test @DisplayName("정상 타입일 때 잔액이 감소한다")
        void payLockerFee_shouldDecreaseBalance() {
            Wallet w = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));
            given(paymentRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.payLockerFee(new LockerPaymentRequest(MEMBER_A, PaymentType.LOCKER_FEE_RENTER, AMOUNT));

            assertThat(w.getBalance()).isZero();
        }

        @Test @DisplayName("잘못된 타입일 때 PaymentNotLockerException 을 던진다")
        void payLockerFee_shouldThrowWhenInvalidType() {
            assertThatThrownBy(() ->
                    service.payLockerFee(new LockerPaymentRequest(MEMBER_A, PaymentType.RENTAL_FEE, AMOUNT))
            ).isInstanceOf(PaymentNotLockerException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("5. 결제내역 조회 (Get Payments)")
    class GetPaymentsTests {

        @Test @DisplayName("조건에 맞는 결제 리스트를 반환한다")
        void getPayments_shouldDelegateToRepo() {
            Payment p = Payment.create(PaymentType.TOP_UP, MEMBER_A, null, AMOUNT, null);
            given(paymentRepo.findByCond(any())).willReturn(List.of(p));

            List<PaymentResponse> list = service.getPayments(new PaymentSearchForm(MEMBER_A, PaymentType.TOP_UP));

            assertThat(list).hasSize(1)
                    .first()
                    .extracting(PaymentResponse::amount)
                    .isEqualTo(AMOUNT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("6. 잔액 확인 (assertCheckBalance)")
    class CheckBalanceTests {

        @Test @DisplayName("충분한 잔액일 때 예외 없음")
        void assertCheckBalance_noException() {
            Wallet w = walletOf(MEMBER_A, AMOUNT);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));

            assertThatCode(() -> service.assertCheckBalance(MEMBER_A, AMOUNT))
                    .doesNotThrowAnyException();
        }

        @Test @DisplayName("잔액 부족 시 InsufficientBalanceException")
        void assertCheckBalance_throwWhenInsufficient() {
            Wallet w = walletOf(MEMBER_A, 100);
            given(walletRepo.findForUpdate(MEMBER_A)).willReturn(Optional.of(w));

            assertThatThrownBy(() -> service.assertCheckBalance(MEMBER_A, AMOUNT))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("7. 사물함 요금 계산 (getLockerFeeByAction / calculateLockerFee)")
    class LockerFeeCalculationTests {

        @Test @DisplayName("calculateLockerFee: 기본 + 시간당 계산")
        void calculateLockerFee_shouldComputeCorrectly() {
            LocalDateTime start = LocalDateTime.now().minusHours(3);
            LocalDateTime end   = LocalDateTime.now();
            long fee = service.calculateLockerFee(start, end);

            assertThat(fee).isEqualTo(1000 + 500 * 3);
        }

        @Test @DisplayName("PICK_UP_BY_RENTER 일 때 LeftAt 기준 계산")
        void getLockerFee_pickUpByRenter() {
            Rental rental = mock(Rental.class);
            LocalDateTime leftAt = LocalDateTime.now().minusHours(2);
            given(rental.getLeftAt()).willReturn(leftAt);
            long fee = service.getLockerFeeByAction(RentalLockerAction.PICK_UP_BY_RENTER, rental, LocalDateTime.now());

            assertThat(fee).isEqualTo(1000 + 500 * 2);
        }

        @Test @DisplayName("RETRIEVE_BY_OWNER 일 때 ReturnedAt 기준 계산")
        void getLockerFee_retrieveByOwner() {
            Rental rental = mock(Rental.class);
            LocalDateTime returnedAt = LocalDateTime.now().minusHours(1);
            given(rental.getReturnedAt()).willReturn(returnedAt);
            long fee = service.getLockerFeeByAction(RentalLockerAction.RETRIEVE_BY_OWNER, rental, LocalDateTime.now());

            assertThat(fee).isEqualTo(1000 + 500 * 1);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("8. 지갑 생성 (createWallet)")
    class CreateWalletTests {

        @Test @DisplayName("새 Wallet 저장 후 memberId 반환")
        void createWallet_shouldSaveAndReturnMemberId() {
            Wallet w = walletOf(MEMBER_A, 0);
            given(walletRepo.save(any())).willReturn(w);

            long result = service.createWallet(MEMBER_A);

            assertThat(result).isEqualTo(MEMBER_A);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // helper to capture saved Payment
    private Payment captureSavedPayment() {
        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepo).should().save(cap.capture());
        return cap.getValue();
    }
}
