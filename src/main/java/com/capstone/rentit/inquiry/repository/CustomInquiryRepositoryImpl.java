package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.domain.QInquiry;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
public class CustomInquiryRepositoryImpl implements CustomInquiryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Inquiry> search(InquirySearchForm form, Pageable pageable) {

        QInquiry q = QInquiry.inquiry;
        BooleanBuilder cond = new BooleanBuilder();

        // --- 필터 조건 빌드 --- //
        cond.and(q.type.eq(form.type()));
        if (form.processed() != null)   cond.and(q.processed.eq(form.processed()));
        if (form.fromDate() != null)    cond.and(q.createdAt.goe(form.fromDate()));
        if (form.toDate() != null)      cond.and(q.createdAt.loe(form.toDate().plusSeconds(10)));

        // --- Query 준비 --- //
        var baseQuery = queryFactory
                .selectFrom(q)
                .where(cond)
                .orderBy(q.createdAt.desc());

        List<Inquiry> content;
        if (pageable.isPaged()) {
            content = baseQuery
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
        } else {
            // unpaged: 전체 조회
            content = baseQuery.fetch();
        }

        // --- total count --- //
        Long total = queryFactory
                .select(q.count())
                .from(q)
                .where(cond)
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content,
                pageable.isPaged() ? pageable : Pageable.ofSize(content.size()),
                totalCount);
    }
}
