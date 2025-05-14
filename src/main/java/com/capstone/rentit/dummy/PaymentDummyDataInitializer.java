package com.capstone.rentit.dummy;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.domain.Wallet;
import com.capstone.rentit.payment.repository.PaymentRepository;
import com.capstone.rentit.payment.repository.WalletRepository;
import com.capstone.rentit.payment.type.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Order(6)
public class PaymentDummyDataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;

    /* ─────────────────────────────────────────────── */

    @Override
    public void run(ApplicationArguments args) throws Exception {

        /* 이미 결제 이력이 있으면 Skip */
        if (paymentRepository.count() > 0) return;

        /* 1) 모든 사용자 지갑을 가져와 “충전” 결제 기록 + 잔액 반영 */
        List<Wallet> wallets = walletRepository.findAll();
        AtomicInteger seq = new AtomicInteger(1);

        wallets.forEach(w -> {
            long amount = 10_000L * seq.get();    // 각 지갑마다 다른 금액
            Payment topUp = Payment.create(
                    PaymentType.TOP_UP,
                    w.getMemberId(),              // from
                    null,                         // to
                    amount);
            topUp.approve("DUMMY-TX-" + "%03d".formatted(seq.getAndIncrement()));

            /* 저장 */
            paymentRepository.save(topUp);

            /* 지갑 잔액 업데이트 */
            w.deposit(amount);
            walletRepository.save(w);
        });

        /* 2) 첫 번째 학생 ↔ 두 번째 학생 간 대여료 & 사물함 요금 예시 */
        List<Member> students = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRoleEnum.STUDENT) // role enum에 맞춰 수정
                .toList();

        if (students.size() >= 2) {
            Long renterId = students.get(0).getMemberId();
            Long ownerId  = students.get(1).getMemberId();

            createInternalTransfer(renterId, ownerId, 3_000L, PaymentType.RENTAL_FEE);
            createInternalTransfer(renterId, 0L,       1_000L, PaymentType.LOCKER_FEE_RENTER);
            createInternalTransfer(ownerId,  0L,       1_000L, PaymentType.LOCKER_FEE_OWNER);
        }

        System.out.printf("[PaymentDummyDataInitializer] Generated %d TOP_UP payments and sample rental/locker fees.%n",
                wallets.size());
    }

    /* ------------------------------------------------------------------ */

    /** 내부 포인트 이동(승인 즉시) + 지갑 반영 */
    private void createInternalTransfer(Long fromId, Long toId, long amount, PaymentType type) {

        Payment p = Payment.create(type, fromId, toId, amount);
        p.approve(null);                 // 내부 이동이므로 외부 거래번호 없음
        paymentRepository.save(p);

        walletRepository.findById(fromId).ifPresent(w -> {
            w.withdraw(amount);
            walletRepository.save(w);
        });

        walletRepository.findById(toId).ifPresent(w -> {
            w.deposit(amount);
            walletRepository.save(w);
        });
    }
}
