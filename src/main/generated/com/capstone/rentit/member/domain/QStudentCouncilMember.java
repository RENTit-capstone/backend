package com.capstone.rentit.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStudentCouncilMember is a Querydsl query type for StudentCouncilMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudentCouncilMember extends EntityPathBase<StudentCouncilMember> {

    private static final long serialVersionUID = -673141486L;

    public static final QStudentCouncilMember studentCouncilMember = new QStudentCouncilMember("studentCouncilMember");

    public final QMember _super = new QMember(this);

    public final StringPath contactEmail = createString("contactEmail");

    //inherited
    public final DatePath<java.time.LocalDate> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    //inherited
    public final StringPath email = _super.email;

    //inherited
    public final StringPath fcmToken = _super.fcmToken;

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

    //inherited
    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> rentedRentals = _super.rentedRentals;

    //inherited
    public final EnumPath<com.capstone.rentit.member.status.MemberRoleEnum> role = _super.role;

    public final StringPath university = createString("university");

    public QStudentCouncilMember(String variable) {
        super(StudentCouncilMember.class, forVariable(variable));
    }

    public QStudentCouncilMember(Path<? extends StudentCouncilMember> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStudentCouncilMember(PathMetadata metadata) {
        super(StudentCouncilMember.class, metadata);
    }

}

