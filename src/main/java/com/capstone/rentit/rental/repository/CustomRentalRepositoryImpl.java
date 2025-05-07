package com.capstone.rentit.rental.repository;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.domain.QRental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomRentalRepositoryImpl implements CustomRentalRepository {

    private final JPAQueryFactory queryFactory;
    private final QRental rental = QRental.rental;

    @Override
    public Page<Rental> findAllByUserIdAndStatuses(Long userId, List<RentalStatusEnum> statuses, Pageable pageable) {
        BooleanExpression predicate = userPredicate(userId)
                .and(statusPredicate(statuses));

        if (pageable.isUnpaged()) {
            List<Rental> all = queryFactory
                    .selectFrom(rental)
                    .where(predicate)
                    .orderBy(defaultOrder())
                    .fetch();
            return new PageImpl<>(all);
        }

        Long total = queryFactory
                .select(rental.count())
                .from(rental)
                .where(predicate)
                .fetchOne();
        long count = (total != null ? total : 0L);

        List<Rental> content = queryFactory
                .selectFrom(rental)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifier(pageable))
                .fetch();

        return new PageImpl<>(content, pageable, count);
    }

    @Override
    public List<Rental> findEligibleRentals(Long memberId, RentalLockerAction action) {
        QRental r = QRental.rental;

        BooleanBuilder b = new BooleanBuilder();

        switch (action) {
            case DROP_OFF_BY_OWNER -> {
                b.and(r.ownerId.eq(memberId))
                        .and(r.status.eq(RentalStatusEnum.APPROVED))
                        .and(r.startDate.before(LocalDateTime.now()));
            }
            case PICK_UP_BY_RENTER -> {
                b.and(r.renterId.eq(memberId))
                        .and(r.status.eq(RentalStatusEnum.LEFT_IN_LOCKER));
            }
            case RETURN_BY_RENTER -> {
                b.and(r.renterId.eq(memberId))
                        .and(r.status.eq(RentalStatusEnum.PICKED_UP));
            }
            case RETRIEVE_BY_OWNER -> {
                b.and(r.ownerId.eq(memberId))
                        .and(r.status.eq(RentalStatusEnum.RETURNED_TO_LOCKER));
            }
        }
        return queryFactory.selectFrom(r).where(b).orderBy(r.requestDate.desc()).fetch();
    }

    private OrderSpecifier<?> orderSpecifier(Pageable pageable) {
        Sort.Order order = pageable.getSort().getOrderFor("requestDate");
        if (order != null) {
            return order.isAscending()
                    ? rental.requestDate.asc()
                    : rental.requestDate.desc();
        }
        return rental.requestDate.desc();
    }

    private OrderSpecifier<?> defaultOrder() {
        return rental.requestDate.desc();
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
