package com.capstone.rentit.rental.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRental is a Querydsl query type for Rental
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRental extends EntityPathBase<Rental> {

    private static final long serialVersionUID = -881894240L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRental rental = new QRental("rental");

    public final DateTimePath<java.time.LocalDateTime> approvedDate = createDateTime("approvedDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> deviceId = createNumber("deviceId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> dueDate = createDateTime("dueDate", java.time.LocalDateTime.class);

    public final com.capstone.rentit.item.domain.QItem item;

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> leftAt = createDateTime("leftAt", java.time.LocalDateTime.class);

    public final com.capstone.rentit.locker.domain.QLocker locker;

    public final NumberPath<Long> lockerId = createNumber("lockerId", Long.class);

    public final NumberPath<Long> ownerId = createNumber("ownerId", Long.class);

    public final com.capstone.rentit.member.domain.QMember ownerMember;

    public final DateTimePath<java.time.LocalDateTime> pickedUpAt = createDateTime("pickedUpAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> rejectedDate = createDateTime("rejectedDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> rentalId = createNumber("rentalId", Long.class);

    public final NumberPath<Long> renterId = createNumber("renterId", Long.class);

    public final com.capstone.rentit.member.domain.QMember renterMember;

    public final DateTimePath<java.time.LocalDateTime> requestDate = createDateTime("requestDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> retrievedAt = createDateTime("retrievedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> returnedAt = createDateTime("returnedAt", java.time.LocalDateTime.class);

    public final StringPath returnImageUrl = createString("returnImageUrl");

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final EnumPath<com.capstone.rentit.rental.status.RentalStatusEnum> status = createEnum("status", com.capstone.rentit.rental.status.RentalStatusEnum.class);

    public QRental(String variable) {
        this(Rental.class, forVariable(variable), INITS);
    }

    public QRental(Path<? extends Rental> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRental(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRental(PathMetadata metadata, PathInits inits) {
        this(Rental.class, metadata, inits);
    }

    public QRental(Class<? extends Rental> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new com.capstone.rentit.item.domain.QItem(forProperty("item"), inits.get("item")) : null;
        this.locker = inits.isInitialized("locker") ? new com.capstone.rentit.locker.domain.QLocker(forProperty("locker"), inits.get("locker")) : null;
        this.ownerMember = inits.isInitialized("ownerMember") ? new com.capstone.rentit.member.domain.QMember(forProperty("ownerMember")) : null;
        this.renterMember = inits.isInitialized("renterMember") ? new com.capstone.rentit.member.domain.QMember(forProperty("renterMember")) : null;
    }

}

