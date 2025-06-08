package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.QMember;
import com.capstone.rentit.member.domain.QStudent;
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
    private final QStudent student = QStudent.student;

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
        Predicate basicFilters               = buildBasicFilters(form);
        BooleanExpression roleFilter         = buildRoleFilter(form.getOwnerRoles());
        BooleanExpression universityFilter   = buildUniversityFilter(form.getUniversity());

        long count = countBy(basicFilters, roleFilter, universityFilter);
        List<Item> content = findContentBy(basicFilters, roleFilter, universityFilter, pageable);

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
        return (roles != null && !roles.isEmpty())
                ? member.role.in(roles)
                : null;
    }

    private BooleanExpression buildUniversityFilter(String university) {
        if (!StringUtils.hasText(university)) {
            return null;
        }
        return student.role.eq(MemberRoleEnum.STUDENT)
                .and(student.university.equalsIgnoreCase(university));
    }

    private long countBy(
            Predicate basicFilter,
            BooleanExpression roleFilter,
            BooleanExpression universityFilter
    ) {
        JPAQuery<Long> q = queryFactory
                .select(item.count())
                .from(item);

        q.join(item.owner, member);

        if (universityFilter != null) {
            q.join(student)
                    .on(member.memberId.eq(student.memberId));
        }

        Predicate combined = ExpressionUtils.allOf(
                basicFilter,
                roleFilter,
                universityFilter
        );
        if (combined != null) {
            q.where(combined);
        }

        Long countResult = q.fetchOne();
        return (countResult != null) ? countResult : 0L;
    }

    private List<Item> findContentBy(
            Predicate basicFilter,
            BooleanExpression roleFilter,
            BooleanExpression universityFilter,
            Pageable pageable
    ) {
        JPAQuery<Item> q = queryFactory
                .select(item)
                .from(item)
                // 1) 항상 item.owner → member FetchJoin
                .join(item.owner, member).fetchJoin();

        // 2) universityFilter가 있을 때만 Student와 ON절로 JOIN
        if (universityFilter != null) {
            q.join(student)
                    .on(member.memberId.eq(student.memberId));
        }

        // 3) roleFilter 적용
        if (roleFilter != null) {
            q.where(roleFilter);
        }

        // 4) universityFilter 적용
        if (universityFilter != null) {
            q.where(universityFilter);
        }

        // 5) 기본 필터 적용
        if (basicFilter != null) {
            q.where(basicFilter);
        }

        // 6) 정렬
        q.orderBy(orderSpecifier(pageable));

        // 7) 페이징
        if (!pageable.isUnpaged()) {
            q.offset(pageable.getOffset());
            q.limit(pageable.getPageSize());
        }

        return q.fetch();
    }

    private OrderSpecifier<?> orderSpecifier(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                boolean asc = order.isAscending();

                if ("price".equals(property)) {
                    return asc ? item.price.asc() : item.price.desc();
                }
                if ("createdAt".equals(property)) {
                    return asc ? item.createdAt.asc() : item.createdAt.desc();
                }
            }
        }
        return item.createdAt.desc();
    }

    private BooleanExpression keywordContains(String kw) {
        if (!StringUtils.hasText(kw)) return null;
        String pattern = "%" + kw.toLowerCase() + "%";
        return item.name.lower().like(pattern)
                .or(item.description.lower().like(pattern));
    }

    private BooleanExpression statusEq(ItemStatusEnum status) {
        return status != null
                ? item.status.eq(status)
                : item.status.ne(ItemStatusEnum.DELETED);
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
