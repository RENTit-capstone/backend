package com.capstone.rentit.payment.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPayment is a Querydsl query type for Payment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPayment extends EntityPathBase<Payment> {

    private static final long serialVersionUID = 1342522080L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPayment payment = new QPayment("payment");

    public final NumberPath<Long> amount = createNumber("amount", Long.class);

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> fromMemberId = createNumber("fromMemberId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath istuno = createString("istuno");

    public final com.capstone.rentit.rental.domain.QRental rental;

    public final EnumPath<com.capstone.rentit.payment.type.PaymentStatus> status = createEnum("status", com.capstone.rentit.payment.type.PaymentStatus.class);

    public final NumberPath<Long> toMemberId = createNumber("toMemberId", Long.class);

    public final EnumPath<com.capstone.rentit.payment.type.PaymentType> type = createEnum("type", com.capstone.rentit.payment.type.PaymentType.class);

    public QPayment(String variable) {
        this(Payment.class, forVariable(variable), INITS);
    }

    public QPayment(Path<? extends Payment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPayment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPayment(PathMetadata metadata, PathInits inits) {
        this(Payment.class, metadata, inits);
    }

    public QPayment(Class<? extends Payment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.rental = inits.isInitialized("rental") ? new com.capstone.rentit.rental.domain.QRental(forProperty("rental"), inits.get("rental")) : null;
    }

}

