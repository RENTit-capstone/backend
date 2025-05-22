package com.capstone.rentit.locker.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDevice is a Querydsl query type for Device
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDevice extends EntityPathBase<Device> {

    private static final long serialVersionUID = -1483065146L;

    public static final QDevice device = new QDevice("device");

    public final NumberPath<Long> deviceId = createNumber("deviceId", Long.class);

    public final StringPath locationDescription = createString("locationDescription");

    public final ListPath<Locker, QLocker> lockers = this.<Locker, QLocker>createList("lockers", Locker.class, QLocker.class, PathInits.DIRECT2);

    public final StringPath university = createString("university");

    public QDevice(String variable) {
        super(Device.class, forVariable(variable));
    }

    public QDevice(Path<? extends Device> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDevice(PathMetadata metadata) {
        super(Device.class, metadata);
    }

}

