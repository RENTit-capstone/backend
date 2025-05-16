package com.capstone.rentit.payment.repository;

import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.domain.QPayment;
import com.capstone.rentit.payment.dto.PaymentResponse;
import com.capstone.rentit.payment.dto.PaymentSearchForm;
import com.capstone.rentit.payment.type.PaymentType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {
    private final JPAQueryFactory queryFactory;
    QPayment p = QPayment.payment;

    @Override
    public List<Payment> findByCond(PaymentSearchForm c) {

        return queryFactory
                .selectFrom(p)
                .where(
                        eqMember(c.memberId(), c.type()),
                        eqType(c.type())
                )
                .orderBy(p.createdAt.desc())
                .fetch();
    }

    /* ---------- where helpers ---------- */
    private BooleanExpression eqMember(Long memberId, PaymentType type) {
        if (memberId == null) return null;
        if (type == PaymentType.LOCKER_FEE_RENTER)
            return p.fromMemberId.eq(memberId);
        return p.fromMemberId.eq(memberId).or(p.toMemberId.eq(memberId));
    }

    private BooleanExpression eqType(PaymentType type) {
        return type == null ? null : p.type.eq(type);
    }
}
