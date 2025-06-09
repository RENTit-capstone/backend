package com.capstone.rentit.payment.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.dto.PaymentSearchForm;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.repository.CustomRentalRepositoryImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class CustomPaymentRepositoryImplTest {
    @Autowired CustomPaymentRepositoryImpl customPaymentRepository;
    @Autowired EntityManager em;

    private static final long MEMBER_A = 1L;
    private static final long MEMBER_B = 2L;

    Payment pay(Long from, Long to, PaymentType type, long amount, int daysAgo) {
        Payment p = Payment.builder()
                .type(type)
                .status(PaymentStatus.APPROVED)
                .fromMemberId(from)
                .toMemberId(to)
                .amount(amount)
                .createdAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
        em.persist(p);
        return p;
    }

    Payment rentalFeeWithRental(Long from, Long to, long amount) {
        /* rental 엔티티 세팅 생략 — 필요 시 stub Rental.builder()… */
        return pay(from, to, PaymentType.RENTAL_FEE, amount, 0);
    }

    @BeforeEach
    void seed() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        customPaymentRepository = new CustomPaymentRepositoryImpl(queryFactory);
        // 최신(0일 전)부터 과거(2일 전)까지 3건
        pay(MEMBER_A, MEMBER_B, PaymentType.TOP_UP,          5000, 0); // idx 0
        pay(MEMBER_B, MEMBER_A, PaymentType.LOCKER_FEE_OWNER,800 ,  1); // idx 1
        rentalFeeWithRental(MEMBER_A, MEMBER_B,              2000);     // idx 2
        em.flush();
    }

    @Nested class FindByCond {

        @Test @DisplayName("필터 없이 전체 조회 & createdAt DESC 정렬")
        void noFilter_returnsAll_desc() {
            List<Payment> list = customPaymentRepository.findByCond(new PaymentSearchForm(null, null));

            assertThat(list)
                    .hasSize(3)
                    .isSortedAccordingTo((a, b) ->
                            b.getCreatedAt().compareTo(a.getCreatedAt())); // desc 검증
        }

        @Test @DisplayName("memberId 로 필터 – from/to 둘 다 매칭")
        void filterByMember() {
            List<Payment> list = customPaymentRepository.findByCond(new PaymentSearchForm(MEMBER_B, null));

            assertThat(list).isNotEmpty()
                    .allSatisfy(p ->
                            assertThat(p.getFromMemberId().equals(MEMBER_B) || p.getToMemberId().equals(MEMBER_B))
                                    .isTrue()
                    );
        }

        @Test @DisplayName("type 으로 필터")
        void filterByType() {
            List<Payment> list = customPaymentRepository.findByCond(
                    new PaymentSearchForm(null, PaymentType.LOCKER_FEE_OWNER));

            assertThat(list)
                    .hasSize(1)
                    .allMatch(p -> p.getType() == PaymentType.LOCKER_FEE_OWNER);
        }

        @Test @DisplayName("memberId + type 동시 필터")
        void filterByBoth() {
            List<Payment> list = customPaymentRepository.findByCond(
                    new PaymentSearchForm(MEMBER_A, PaymentType.TOP_UP));

            assertThat(list)
                    .hasSize(1)
                    .first()
                    .satisfies(p ->
                            assertThat(p.getFromMemberId()).isEqualTo(MEMBER_A));
        }
    }
}