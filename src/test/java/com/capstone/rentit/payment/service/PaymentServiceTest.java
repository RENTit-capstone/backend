package com.capstone.rentit.payment.service;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.payment.config.NhApiProperties;
import com.capstone.rentit.payment.domain.*;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.exception.*;
import com.capstone.rentit.payment.repository.*;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock  WalletRepository walletRepository;
    @Mock  PaymentRepository paymentRepository;
    @Mock  NhApiClient nhApiClient;
    @Mock  NhApiProperties prop;
    @InjectMocks PaymentService paymentService;

    private static final long MEMBER_A = 1L;
    private static final long MEMBER_B = 2L;
    private static final long AMOUNT   = 10_000L;

    private static Wallet walletOf(long memberId, long balance) {
        return Wallet.builder().memberId(memberId).balance(balance)
                .finAcno("12345678910").bankCode("000")
                .consentAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusYears(1)).build();
    }

    @Nested class TopUpTests {

        @Test void topUp_success_deposits_existingWallet() {
            // given
            Wallet wallet = walletOf(MEMBER_A, 0);

            given(walletRepository.findForUpdate(MEMBER_A))
                    .willReturn(Optional.of(wallet));

            given(paymentRepository.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DrawingTransferResponse resp = new DrawingTransferResponse(
                    mock(NhHeader.class),  // 헤더 상세값 필요 없으면 mock
                    wallet.getFinAcno(),
                    "20250515"
            );
            given(nhApiClient.drawingTransfer(eq(wallet.getFinAcno()), eq(AMOUNT), anyString()))
                    .willReturn(resp);

            ArgumentCaptor<Payment> payCap = ArgumentCaptor.forClass(Payment.class);

            // when
            paymentService.topUp(new TopUpRequest(MEMBER_A, AMOUNT));

            // then
            assertThat(wallet.getBalance()).isEqualTo(AMOUNT);      // 1) 지갑 증가 확인

            then(paymentRepository).should().save(payCap.capture()); // 2) save 호출 검증
            Payment saved = payCap.getValue();

            assertThat(saved.getType()).isEqualTo(PaymentType.TOP_UP);
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(saved.getAmount()).isEqualTo(AMOUNT);
            assertThat(saved.getFromMemberId()).isEqualTo(MEMBER_A);
        }

        @Test void topUp_walletNotFound_throws() {
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.empty());
            assertThatThrownBy(() ->
                    paymentService.topUp(new TopUpRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }

        @Test void topUp_externalFail_throws_and_noDeposit() {
            // given
            Wallet wallet = walletOf(MEMBER_A, 0);

            given(walletRepository.findForUpdate(MEMBER_A))
                    .willReturn(Optional.of(wallet));

            given(paymentRepository.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            given(nhApiClient.drawingTransfer(anyString(), anyLong(), anyString()))
                    .willReturn(null);

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
            given(walletRepository.findForUpdate(MEMBER_A))
                    .willReturn(Optional.of(wallet));                                   // id 세팅

            given(paymentRepository.save(any(Payment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            DepositResponse resp = new DepositResponse(
                    mock(NhHeader.class),          // 헤더 값 필요 없으면 mock
                    wallet.getFinAcno(),
                    "20250515"
            );
            given(nhApiClient.deposit(anyString(), anyLong(), anyString()))
                    .willReturn(resp);
            ArgumentCaptor<Payment> payCap = ArgumentCaptor.forClass(Payment.class);

            // when
            paymentService.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT));

            // then
            assertThat(wallet.getBalance()).isZero();           // 1) 지갑 차감 확인
            then(paymentRepository).should().save(payCap.capture());

            Payment saved = payCap.getValue();                  // 2) 저장된 Payment 검증
            assertThat(saved.getType()).isEqualTo(PaymentType.WITHDRAWAL);
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(saved.getAmount()).isEqualTo(AMOUNT);
        }

        @Test void withdraw_walletNotFound_throws() {
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.empty());
            assertThatThrownBy(() ->
                    paymentService.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(WalletNotFoundException.class);
        }

        @Test void withdraw_insufficientBalance_throws() {
            Wallet wallet = walletOf(MEMBER_A, 1_000);
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));
            assertThatThrownBy(() ->
                    paymentService.withdraw(new WithdrawRequest(MEMBER_A, AMOUNT)))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    @Nested class RentalFeeTests {

        @Test void payRentalFee_movesMoney_betweenMembers() {
            // given
            Wallet renter = walletOf(MEMBER_A, AMOUNT);
            Wallet owner  = walletOf(MEMBER_B, 0);
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(walletRepository.findForUpdate(MEMBER_B)).willReturn(Optional.of(owner));
            given(paymentRepository.save(any(Payment.class)))
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
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(renter));
            given(paymentRepository.save(any(Payment.class)))
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

    @Nested class AssertBalanceTests {

        @Test void assertCheckBalance_success() {
            // given – 잔액이 충분한 지갑
            Wallet wallet = walletOf(MEMBER_A, AMOUNT);
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));

            // when / then – 예외가 발생하지 않아야 한다
            paymentService.assertCheckBalance(MEMBER_A, AMOUNT);
        }

        @Test void assertCheckBalance_insufficient_throws() {
            // given – 잔액 부족
            Wallet wallet = walletOf(MEMBER_A, 1_000);
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));

            // when / then
            assertThatThrownBy(() ->
                    paymentService.assertCheckBalance(MEMBER_A, AMOUNT))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test void assertCheckBalance_invalidAmount_throws() {
            Wallet wallet = walletOf(MEMBER_A, AMOUNT);
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.of(wallet));

            assertThatThrownBy(() ->
                    paymentService.assertCheckBalance(MEMBER_A, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test void assertCheckBalance_walletNotFound_throws() {
            given(walletRepository.findForUpdate(MEMBER_A)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    paymentService.assertCheckBalance(MEMBER_A, AMOUNT))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }

    @Nested class CalculateLockerFeeTests {

        @Test @DisplayName("0시간→ 기본요금만 청구")
        void zeroHours_returnsBasic() {
            LocalDateTime start = LocalDateTime.of(2025, 5, 1, 10, 0);
            LocalDateTime end   = start;                          // 0h
            long fee = paymentService.calculateLockerFee(start, end);

            assertThat(fee).isEqualTo(1000);                      // LOCKER_FEE_BASIC
        }

        @Test @DisplayName("2시간→ 기본요금 + 2×시간당요금")
        void twoHours() {
            LocalDateTime start = LocalDateTime.of(2025, 5, 1, 8, 0);
            LocalDateTime end   = start.plusHours(2);             // 2h
            long fee = paymentService.calculateLockerFee(start, end);

            assertThat(fee).isEqualTo(1000 + 2 * 500);            // B + 2×P
        }

        @Test @DisplayName("종료가 시작보다 이르면 음수시간: Duration 음수 처리 ‑ 예외 없음")
        void negativeDuration_stillCalculates() {
            LocalDateTime start = LocalDateTime.of(2025, 5, 1, 10, 0);
            LocalDateTime end   = start.minusHours(1);            // ‑1h
            long fee = paymentService.calculateLockerFee(start, end);

            // Duration#toHours 가 음수 반환 → 기본요금보다 작을 수 있음
            assertThat(fee).isEqualTo(1000 + (-1) * 500);
        }
    }

    @Nested class GetLockerFeeByActionTests {


        /** 공통 Rental 픽스처 */
        private Rental rentalWithTimes(LocalDateTime leftAt, LocalDateTime returnedAt) {
            return Rental.builder()
                    .rentalId(1L)
                    .leftAt(leftAt)
                    .returnedAt(returnedAt)
                    .build();
        }

        @Test @DisplayName("대여자 픽업 ‑ leftAt 기준 계산")
        void pickUpByRenter() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime leftAt = now.minusHours(3);             // 3h 경과
            Rental rental = rentalWithTimes(leftAt, null);

            long fee = paymentService.getLockerFeeByAction(
                    RentalLockerAction.PICK_UP_BY_RENTER, rental, now);
            assertThat(fee).isEqualTo(1000 + 3 * 500);
        }

        @Test @DisplayName("소유자 회수 ‑ returnedAt 기준 계산")
        void retrieveByOwner() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime returnedAt = now.minusHours(1);         // 1h 경과
            Rental rental = rentalWithTimes(null, returnedAt);

            long fee = paymentService.getLockerFeeByAction(
                    RentalLockerAction.RETRIEVE_BY_OWNER, rental, now);
            assertThat(fee).isEqualTo(1000 + 1 * 500);
        }
    }

    @Nested class CreateWalletTests {

        @Test @DisplayName("새 지갑을 저장하고 memberId 반환")
        void createWallet_savesAndReturnsId() {
            long memberId = 99L;
            Wallet saved = Wallet.builder()
                    .memberId(memberId)
                    .balance(0L)
                    .build();

            given(walletRepository.save(any(Wallet.class))).willReturn(saved);

            Long returnedId = paymentService.createWallet(memberId);

            then(walletRepository).should()
                    .save(argThat(w -> w.getMemberId().equals(memberId) && w.getBalance() == 0));
            assertThat(returnedId).isEqualTo(memberId);
        }
    }
}
