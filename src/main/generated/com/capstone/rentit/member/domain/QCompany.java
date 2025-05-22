package com.capstone.rentit.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCompany is a Querydsl query type for Company
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCompany extends EntityPathBase<Company> {

    private static final long serialVersionUID = 1193862219L;

    public static final QCompany company = new QCompany("company");

    public final QMember _super = new QMember(this);

    public final StringPath companyName = createString("companyName");

    public final StringPath contactEmail = createString("contactEmail");

    //inherited
    public final DatePath<java.time.LocalDate> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    //inherited
    public final StringPath email = _super.email;

    //inherited
    public final StringPath fcmToken = _super.fcmToken;

    public final StringPath industry = createString("industry");

    //inherited
    public final SetPath<com.capstone.rentit.item.domain.Item, com.capstone.rentit.item.domain.QItem> items = _super.items;

    //inherited
    public final BooleanPath locked = _super.locked;

    //inherited
    public final NumberPath<Long> memberId = _super.memberId;

    //inherited
    public final StringPath name = _super.name;

    //inherited
    public final StringPath nickname = _super.nickname;

    //inherited
    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> ownedRentals = _super.ownedRentals;

    //inherited
    public final StringPath password = _super.password;

    //inherited
    public final StringPath profileImg = _super.profileImg;

    public final StringPath registrationNumber = createString("registrationNumber");

    //inherited
    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> rentedRentals = _super.rentedRentals;

    //inherited
    public final EnumPath<com.capstone.rentit.member.status.MemberRoleEnum> role = _super.role;

    public QCompany(String variable) {
        super(Company.class, forVariable(variable));
    }

    public QCompany(Path<? extends Company> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCompany(PathMetadata metadata) {
        super(Company.class, metadata);
    }

}

