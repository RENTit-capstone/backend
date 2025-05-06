package com.capstone.rentit.member.repository;

import com.capstone.rentit.item.domain.QItem;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.QMember;
import com.capstone.rentit.rental.domain.QRental;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomMemberRepositoryImpl implements CustomMemberRepository{
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Member> findProfileWithAll(Long memberId) {

        QMember m   = QMember.member;
        QItem i   = QItem.item;
        QRental orr = new QRental("ownedRental");
        QRental rr  = new QRental("rentedRental");
        QItem   oi  = new QItem("ownedItem");
        QItem   ri  = new QItem("rentedItem");

        Member fetched = queryFactory
                .selectDistinct(m)                          // 중복 제거
                .from(m)
                .leftJoin(m.items, i).fetchJoin()           // ① 내가 등록한 물품
                .leftJoin(m.ownedRentals, orr).fetchJoin()  // ② 소유자로서의 대여
                .leftJoin(orr.item, oi).fetchJoin()         //    해당 대여가 참조하는 Item
                .leftJoin(m.rentedRentals, rr).fetchJoin()  // ③ 대여자로서의 대여
                .leftJoin(rr.item, ri).fetchJoin()          //    해당 대여가 참조하는 Item
                .where(m.memberId.eq(memberId))
                /* Hibernate가 DISTINCT 구문을 그대로 전달하지 않게 하는 hint
                   (중복 엔티티 필터링은 JPA 레벨에서 수행)                       */
                .setHint("hibernate.query.passDistinctThrough", false)
                .fetchOne();

        return Optional.ofNullable(fetched);
    }
}
