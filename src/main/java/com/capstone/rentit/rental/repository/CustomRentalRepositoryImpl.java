package com.capstone.rentit.rental.repository;

import com.capstone.rentit.item.domain.QItem;
import com.capstone.rentit.locker.domain.QLocker;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.member.domain.QMember;
import com.capstone.rentit.rental.domain.QRental;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
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
    public Page<Rental> findAllByUserIdAndStatuses(Long userId,
                                                   List<RentalStatusEnum> statuses,
                                                   Pageable pageable) {
        // predicate
        BooleanExpression predicate = userPredicate(userId)
                .and(statusPredicate(statuses));

        // aliases for fetch join
        QMember owner  = new QMember("owner");
        QMember renter = new QMember("renter");
        QItem   item   = QItem.item;
        QLocker locker  = QLocker.locker;

        // base query with fetch joins
        JPAQuery<Rental> base = queryFactory
                .selectFrom(rental)
                .leftJoin(rental.ownerMember, owner).fetchJoin()
                .leftJoin(rental.renterMember, renter).fetchJoin()
                .leftJoin(rental.item, item).fetchJoin()
                .leftJoin(rental.locker, locker).fetchJoin()
                .where(predicate);

        // unpaged
        if (pageable.isUnpaged()) {
            List<Rental> all = base.orderBy(defaultOrder()).fetch();
            return new PageImpl<>(all);
        }

        // total count without fetch join
        Long total = queryFactory
                .select(rental.count())
                .from(rental)
                .where(predicate)
                .fetchOne();
        long count = total != null ? total : 0L;

        // content with paging
        List<Rental> content = base
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifier(pageable))
                .fetch();

        return new PageImpl<>(content, pageable, count);
    }

    @Override
    public Page<Rental> findAllByStatuses(List<RentalStatusEnum> statuses, Pageable pageable) {
        // 1) predicate 생성: 상태만 조건. statuses 가 비어있으면 null → where 절 생략
        BooleanExpression predicate = statusPredicate(statuses);

        // 2) fetch join 에 사용할 alias 정의 (user join, item join, locker join 모두 동일하게 fetch)
        QMember owner   = new QMember("owner");
        QMember renter  = new QMember("renter");
        QItem   item    = QItem.item;
        QLocker locker  = QLocker.locker;

        // 3) fetch join 을 포함한 기본 JPAQuery (조건은 statusPredicate 만)
        JPAQuery<Rental> base = queryFactory
                .selectFrom(rental)
                .leftJoin(rental.ownerMember, owner).fetchJoin()
                .leftJoin(rental.renterMember, renter).fetchJoin()
                .leftJoin(rental.item, item).fetchJoin()
                .leftJoin(rental.locker, locker).fetchJoin()
                .where(predicate);

        // --- Unpaged일 경우, 전체 fetch 후 PageImpl 반환 ---
        if (pageable.isUnpaged()) {
            List<Rental> all = base.orderBy(defaultOrder()).fetch();
            return new PageImpl<>(all);
        }

        // --- 총 개수 계산 (fetchJoin 제외) ---
        Long total = queryFactory
                .select(rental.count())
                .from(rental)
                .where(predicate)
                .fetchOne();
        long count = (total != null ? total : 0L);

        // --- 실제 content: 페이징(offset/limit) + 정렬(orderBy) 적용 ---
        List<Rental> content = base
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifier(pageable))
                .fetch();

        return new PageImpl<>(content, pageable, count);
    }

    @Override
    public List<Rental> findEligibleRentals(Long memberId, RentalLockerAction action) {
        QRental r = QRental.rental;
        BooleanBuilder builder = new BooleanBuilder();

        switch (action) {
            case DROP_OFF_BY_OWNER -> builder
                    .and(r.ownerId.eq(memberId))
                    .and(r.status.eq(RentalStatusEnum.APPROVED))
                    .and(r.startDate.before(LocalDateTime.now()));
            case PICK_UP_BY_RENTER -> builder
                    .and(r.renterId.eq(memberId))
                    .and(r.status.eq(RentalStatusEnum.LEFT_IN_LOCKER));
            case RETURN_BY_RENTER -> builder
                    .and(r.renterId.eq(memberId))
                    .and(r.status.eq(RentalStatusEnum.PICKED_UP));
            case RETRIEVE_BY_OWNER -> builder
                    .and(r.ownerId.eq(memberId))
                    .and(r.status.eq(RentalStatusEnum.RETURNED_TO_LOCKER));
        }
        return queryFactory
                .selectFrom(r)
                .where(builder)
                .orderBy(r.requestDate.desc())
                .fetch();
    }

    private OrderSpecifier<?> orderSpecifier(Pageable pageable) {
        if (!pageable.getSort().iterator().hasNext()) {
            return rental.requestDate.desc();
        }

        // 첫 번째 Sort.Order 정보를 사용
        Sort.Order order = pageable.getSort().iterator().next();
        String property = order.getProperty();
        boolean asc = order.getDirection().isAscending();

        return switch (property) {
            case "requestDate" -> asc ? rental.requestDate.asc() : rental.requestDate.desc();
            case "startDate" -> asc ? rental.startDate.asc() : rental.startDate.desc();
            case "dueDate" -> asc ? rental.dueDate.asc() : rental.dueDate.desc();
            case "approvedDate" -> asc ? rental.approvedDate.asc() : rental.approvedDate.desc();
            case "rejectedDate" -> asc ? rental.rejectedDate.asc() : rental.rejectedDate.desc();
            case "leftAt" -> asc ? rental.leftAt.asc() : rental.leftAt.desc();
            case "pickedUpAt" -> asc ? rental.pickedUpAt.asc() : rental.pickedUpAt.desc();
            case "returnedAt" -> asc ? rental.returnedAt.asc() : rental.returnedAt.desc();
            case "retrievedAt" -> asc ? rental.retrievedAt.asc() : rental.retrievedAt.desc();
            default ->
                    rental.requestDate.desc();
        };
    }

    private OrderSpecifier<?> defaultOrder() {
        return rental.requestDate.desc();
    }

    private BooleanExpression userPredicate(Long userId) {
        return rental.ownerId.eq(userId)
                .or(rental.renterId.eq(userId));
    }

    private BooleanExpression statusPredicate(List<RentalStatusEnum> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            return null;
        }
        return rental.status.in(statuses);
    }
}
