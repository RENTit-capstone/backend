package com.capstone.rentit.rental.repository;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.domain.QRental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomRentalRepositoryImpl implements CustomRentalRepository {

    private final JPAQueryFactory queryFactory;
    private final QRental rental = QRental.rental;

    @Override
    public List<Rental> findAllByUserIdAndStatuses(Long userId, List<RentalStatusEnum> statuses) {
        return queryFactory
                .selectFrom(rental)
                .where(
                        userPredicate(userId),
                        statusPredicate(statuses)
                )
                .fetch();
    }

    // “소유자 or 대여자” 조건
    private BooleanExpression userPredicate(Long userId) {
        return rental.ownerId.eq(userId)
                .or(rental.renterId.eq(userId));
    }

    // 상태 리스트가 비어 있지 않을 때만 in 조건 적용
    private BooleanExpression statusPredicate(List<RentalStatusEnum> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            return null;
        }
        return rental.status.in(statuses);
    }
}
