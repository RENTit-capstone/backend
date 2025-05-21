package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.QMember;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.querydsl.core.BooleanBuilder;
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
import java.util.Optional;

import static com.capstone.rentit.member.domain.QMember.member;

@Repository
@RequiredArgsConstructor
public class CustomItemRepositoryImpl implements CustomItemRepository{

    private final JPAQueryFactory queryFactory;
    private final QItem item = QItem.item;
    private final QMember member = QMember.member;

    @Override
    public Optional<Item> findWithOwnerByItemId(Long itemId) {
        Item found = queryFactory
                .select(item)
                .from(item)
                .join(item.owner, member).fetchJoin()
                .where(item.itemId.eq(itemId))
                .fetchOne();

        return Optional.ofNullable(found);
    }

    @Override
    public Page<Item> search(ItemSearchForm form, Pageable pageable) {
        Predicate filters = buildBasicFilters(form);
        BooleanExpression roleFilter  = buildRoleFilter(form.getOwnerRoles());

        long count = countBy(filters, roleFilter);
        List<Item> content = findContentBy(filters, roleFilter, pageable);

        return new PageImpl<>(content, pageable, count);
    }

    private Predicate buildBasicFilters(ItemSearchForm form) {
        return ExpressionUtils.allOf(
                keywordContains(form.getKeyword()),
                startDateGoe(form.getStartDate()),
                endDateLoe(form.getEndDate()),
                priceGoe(form.getMinPrice()),
                priceLoe(form.getMaxPrice()),
                statusEq(form.getStatus())
        );
    }

    private BooleanExpression buildRoleFilter(List<MemberRoleEnum> roles) {
        return (roles != null && !roles.isEmpty()) ? member.role.in(roles) : null;
    }

    private long countBy(Predicate filters, BooleanExpression roleFilter) {
        JPAQuery<Long> q = queryFactory
                .select(item.count())
                .from(item);

        if (roleFilter != null) {
            q.join(item.owner, member)
                    .where(roleFilter);
        }

        Long c = q.where(filters).fetchOne();
        return c != null ? c : 0L;
    }

    private List<Item> findContentBy(Predicate filters,
                                     BooleanExpression roleFilter,
                                     Pageable pageable) {

        JPAQuery<Item> q = queryFactory
                .select(item)
                .from(item);

        q.join(item.owner, member).fetchJoin();

        if (roleFilter != null) {
            q.where(roleFilter);
        }

        q.where(filters)
                .orderBy(orderSpecifier(pageable));

        if (!pageable.isUnpaged()) {
            q.offset(pageable.getOffset())
                    .limit(pageable.getPageSize());
        }

        return q.fetch();
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
}
