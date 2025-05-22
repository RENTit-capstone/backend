package com.capstone.rentit.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QStudent is a Querydsl query type for Student
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudent extends EntityPathBase<Student> {

    private static final long serialVersionUID = -1635767799L;

    public static final QStudent student = new QStudent("student");

    public final QMember _super = new QMember(this);

    //inherited
    public final DatePath<java.time.LocalDate> createdAt = _super.createdAt;

    //inherited
    public final StringPath email = _super.email;

    //inherited
    public final StringPath fcmToken = _super.fcmToken;

    public final EnumPath<com.capstone.rentit.member.status.GenderEnum> gender = createEnum("gender", com.capstone.rentit.member.status.GenderEnum.class);

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

    public final StringPath phone = createString("phone");

    //inherited
    public final StringPath profileImg = _super.profileImg;

    //inherited
    public final SetPath<com.capstone.rentit.rental.domain.Rental, com.capstone.rentit.rental.domain.QRental> rentedRentals = _super.rentedRentals;

    //inherited
    public final EnumPath<com.capstone.rentit.member.status.MemberRoleEnum> role = _super.role;

    public final StringPath studentId = createString("studentId");

    public final StringPath university = createString("university");

    public QStudent(String variable) {
        super(Student.class, forVariable(variable));
    }

    public QStudent(Path<? extends Student> path) {
        super(path.getType(), path.getMetadata());
    }

    public QStudent(PathMetadata metadata) {
        super(Student.class, metadata);
    }

}

