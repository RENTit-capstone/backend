package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.capstone.rentit.item.domain.QItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomItemRepositoryImpl implements CustomItemRepository{

    private final JPAQueryFactory queryFactory;
    private final QItem item = QItem.item;

    @Override
    public List<Item> search(ItemSearchForm form) {
        return queryFactory
                .selectFrom(item)
                .where(
                        keywordContains(form.getKeyword()),
                        startDateGoe(form.getStartDate()),
                        endDateLoe(form.getEndDate()),
                        priceGoe(form.getMinPrice()),
                        priceLoe(form.getMaxPrice())
                )
                .orderBy(item.createdAt.desc())
                .fetch();
    }

    private BooleanExpression keywordContains(String kw) {
        if (!StringUtils.hasText(kw)) return null;
        String pattern = "%" + kw.toLowerCase() + "%";
        // name 또는 description 중 하나라도 매칭
        return item.name.lower().like(pattern)
                .or(item.description.lower().like(pattern));
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
