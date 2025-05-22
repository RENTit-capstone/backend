package com.capstone.rentit.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = 1839575308L;

    public static final QMember member = new QMember("member1");

    public final DatePath<java.time.LocalDate> createdAt = createDate("createdAt", java.time.LocalDate.class);

    public final StringPath email = createString("email");

    public final StringPath fcmToken = createString("fcmToken");

    public final SetPath<com.capstone.rentit.item.domain.Item, com.capstone.rentit.item.domain.QItem> items = this.<com.capstone.rentit.item.domain.Item, com.capstone.rentit.item.domain.QItem>createSet("items", com.capstone.rentit.item.domain.Item.class, com.capstone.rentit.item.domain.QItem.class, PathInits.DIRECT2);

    public final BooleanPath locked = createBoolean("locked");

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> ownedRentals = this.<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental>createSet("ownedRentals", com.capstone.rentit.rental.domain.Rental.class, com.capstone.rentit.rental.domain.QRental.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final StringPath profileImg = createString("profileImg");

    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> rentedRentals = this.<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental>createSet("rentedRentals", com.capstone.rentit.rental.domain.Rental.class, com.capstone.rentit.rental.domain.QRental.class, PathInits.DIRECT2);

    public final EnumPath<com.capstone.rentit.member.status.MemberRoleEnum> role = createEnum("role", com.capstone.rentit.member.status.MemberRoleEnum.class);

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

