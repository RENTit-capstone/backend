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

    /* 샌드박스용 NH 핀-어카운트 더미 생성기 */
    private String fakeFinAcno(long memberId) {
        // 1 + (은행코드 011) + memberId(왼쪽 0-pad, 13자리) + 체크숫자 6자리
        return "1" + "011" + String.format("%013d", memberId) + "000000";
    }

    @Override
    public void run(ApplicationArguments args) {
//        if(paymentRepository.count() > 0) return;;

        /* ─── 0. 지갑 준비 & 계좌 등록 ─── */
        List<Member> members = memberRepository.findAll();
        members.forEach(m -> {
            Wallet w = walletRepository.findById(m.getMemberId())
                    .orElseGet(() -> walletRepository.save(
                            Wallet.builder()
                                    .memberId(m.getMemberId())
                                    .balance(0L)
                                    .build()));

            /* finAcno 가 없으면 더미 계좌 등록 */
            if (w.getFinAcno() == null) {
                w.registerAccount(
                        fakeFinAcno(m.getMemberId()),
                        "011"                   // 농협 코드
                );
                walletRepository.save(w);
            }
        });

        /* ─── 1. 더미 결제 내역 생성 (TOP_UP) ─── */
        if (paymentRepository.count() == 0) {
            AtomicInteger seq = new AtomicInteger(1);
            walletRepository.findAll().forEach(w -> {
                long amt = 10_000L * seq.get();
                Payment topUp = Payment.create(
                        PaymentType.TOP_UP, w.getMemberId(), null, amt, null);
                topUp.approve("DUMMY-" + seq.getAndIncrement());
                paymentRepository.save(topUp);
                w.deposit(amt);
                walletRepository.save(w);
            });
        }

        /* ─── 2. 샘플 대여/사물함 비용 ─── */
        List<Member> students = members.stream()
                .filter(m -> m.getRole() == MemberRoleEnum.STUDENT)
                .toList();
        if (students.size() >= 2) {
            Long renter = students.get(0).getMemberId();
            Long owner  = students.get(1).getMemberId();
            createInternal(renter, owner, 3_000L, PaymentType.RENTAL_FEE);
            createInternal(renter, 0L,     1_000L, PaymentType.LOCKER_FEE_RENTER);
            createInternal(owner,  0L,     1_000L, PaymentType.LOCKER_FEE_OWNER);
        }

        System.out.println("[Dummy] Wallet & Payment seed completed.");
    }

    /* 내부 포인트 이동 */
    private void createInternal(Long from, Long to, long amt, PaymentType type) {
        Payment p = Payment.create(type, from, to, amt, null);
        p.approve(null);
        paymentRepository.save(p);

        walletRepository.findById(from).ifPresent(w -> { w.withdraw(amt); walletRepository.save(w); });
        walletRepository.findById(to).ifPresent(w -> { w.deposit(amt);  walletRepository.save(w); });
    }
}
