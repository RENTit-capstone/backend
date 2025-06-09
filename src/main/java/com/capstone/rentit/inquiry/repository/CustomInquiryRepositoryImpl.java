package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.domain.QInquiry;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

import java.util.List;

@RequiredArgsConstructor
public class CustomInquiryRepositoryImpl implements CustomInquiryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Inquiry> search(InquirySearchForm form, MemberRoleEnum role, Long memberId, Pageable pageable) {

        QInquiry q = QInquiry.inquiry;
        BooleanBuilder cond = new BooleanBuilder();

        /* -------- 필터 조건 -------- */
        if (form.type() != null) {
            cond.and(q.type.eq(form.type()));
        }

        if (form.processed() != null) {
            cond.and(q.processed.eq(form.processed()));
        }

        /* 날짜 범위 */
        if (form.fromDate() != null && form.toDate() != null) {
            cond.and(q.createdAt.between(form.fromDate(), form.toDate()));
        } else if (form.fromDate() != null) {
            cond.and(q.createdAt.goe(form.fromDate()));
        } else if (form.toDate() != null) {
            cond.and(q.createdAt.loe(form.toDate()));
        }

        /* 🔹 userId 가 있으면 “작성자 or 대상자” 모두 포함 */
        if (role != MemberRoleEnum.ADMIN) {
            cond.and(
                    q.memberId.eq(memberId)
                            .or(q.targetMemberId.eq(memberId))   // DAMAGE 신고의 피신고자
            );
        }

        /* -------- 조회 -------- */
        var base = queryFactory
                .selectFrom(q)
                .where(cond)
                .orderBy(q.createdAt.desc());

        List<Inquiry> content = pageable.isPaged()
                ? base.offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                : base.fetch();

        long total = queryFactory
                .select(q.count())
                .from(q)
                .where(cond)
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable.isPaged() ? pageable : Pageable.unpaged(),
                total
        );
    }
}
