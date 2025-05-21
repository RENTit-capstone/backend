package com.capstone.rentit.dummy;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.payment.domain.Wallet;
import com.capstone.rentit.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(5)
public class WalletDummyDataInitializer implements ApplicationRunner {

    private static final long INIT_BALANCE_STUDENT  = 20_000L;
    private static final long INIT_BALANCE_COMPANY  = 100_000L;
    private static final long INIT_BALANCE_COUNCIL  = 50_000L;

    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        walletRepository.deleteAllInBatch();

        List<Member> members = memberRepository.findAll();

        for (Member m : members) {
            long initial = switch (m.getRole()) {
                case STUDENT  -> INIT_BALANCE_STUDENT;
                case COMPANY  -> INIT_BALANCE_COMPANY;
                case COUNCIL  -> INIT_BALANCE_COUNCIL;
                default       -> 0L;
            };

            Wallet wallet = Wallet.builder()
                    .memberId(m.getMemberId())
                    .balance(initial)
                    .build();

            walletRepository.save(wallet);
        }

        System.out.printf("[WalletDummyDataInitializer] Generated %d wallets.%n", members.size());
    }
}
