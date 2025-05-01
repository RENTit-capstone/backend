package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.QMember;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.capstone.rentit.item.domain.QItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.capstone.rentit.member.domain.QMember.member;

@Repository
@RequiredArgsConstructor
public class CustomItemRepositoryImpl implements CustomItemRepository{

    private final JPAQueryFactory queryFactory;
    private final QItem item = QItem.item;
    private final QMember member = QMember.member;

    @Override
    public Page<Item> search(ItemSearchForm form, Pageable pageable) {
        Predicate predicate = ExpressionUtils.allOf(
                keywordContains(form.getKeyword()),
                startDateGoe(form.getStartDate()),
                endDateLoe(form.getEndDate()),
                priceGoe(form.getMinPrice()),
                priceLoe(form.getMaxPrice()),
                statusEq(form.getStatus())
        );

        boolean hasRoleFilter = form.getOwnerRoles() != null && !form.getOwnerRoles().isEmpty();

        if (pageable.isUnpaged()) {
            JPAQuery<Item> uq = queryFactory.selectFrom(item);
            uq = applyOwnerAndRoles(uq, form.getOwnerRoles());
            List<Item> all = uq
                    .where(predicate)
                    .orderBy(defaultOrder())
                    .fetch();
            return new PageImpl<>(all);
        }

        JPAQuery<Long> countQ = queryFactory.select(item.count()).from(item);
        countQ = applyOwnerAndRoles(countQ, form.getOwnerRoles());
        Long total = countQ.where(predicate).fetchOne();
        long count = total != null ? total : 0L;

        JPAQuery<Item> contentQ = queryFactory.selectFrom(item);
        contentQ = applyOwnerAndRoles(contentQ, form.getOwnerRoles());
        List<Item> content = contentQ
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(orderSpecifier(pageable))
                .fetch();

        return new PageImpl<>(content, pageable, count);
    }

    private OrderSpecifier<?> defaultOrder() {
        return item.createdAt.desc();
    }

    private OrderSpecifier<?> orderSpecifier(Pageable pageable) {
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        if (order != null) {
            return order.isAscending()
                    ? item.createdAt.asc()
                    : item.createdAt.desc();
        }
        return defaultOrder();
    }

    private BooleanExpression keywordContains(String kw) {
        if (!StringUtils.hasText(kw)) return null;
        String pattern = "%" + kw.toLowerCase() + "%";
        // name 또는 description 중 하나라도 매칭
        return item.name.lower().like(pattern)
                .or(item.description.lower().like(pattern));
    }

    private BooleanExpression statusEq(ItemStatusEnum status) {
        return status != null
                ? item.status.eq(status)
                : null;
    }

    private BooleanExpression startDateGoe(LocalDateTime date) {
        return date != null ? item.startDate.goe(date) : null;
    }

    private BooleanExpression endDateLoe(LocalDateTime date) {
        return date != null ? item.endDate.loe(date) : null;
    }

    private BooleanExpression priceGoe(Integer min) {
        return min != null ? item.price.goe(min) : null;
    }

    private BooleanExpression priceLoe(Integer max) {
        return max != null ? item.price.loe(max) : null;
    }

    @SuppressWarnings("unchecked")
    private <T> JPAQuery<T> applyOwnerAndRoles(JPAQuery<T> query, List<MemberRoleEnum> roles) {
        if (roles != null && !roles.isEmpty()) {
            query = (JPAQuery<T>) query
                    .join(item.owner, member)
                    .where(member.role.in(roles));
        }
        return query;
    }
}
