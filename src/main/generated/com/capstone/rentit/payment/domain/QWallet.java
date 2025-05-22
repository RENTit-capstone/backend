package com.capstone.rentit.payment.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QWallet is a Querydsl query type for Wallet
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWallet extends EntityPathBase<Wallet> {

    private static final long serialVersionUID = 104775647L;

    public static final QWallet wallet = new QWallet("wallet");

    public final NumberPath<Long> balance = createNumber("balance", Long.class);

    public final StringPath bankCode = createString("bankCode");

    public final DateTimePath<java.time.LocalDateTime> consentAt = createDateTime("consentAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final StringPath finAcno = createString("finAcno");

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    public QWallet(String variable) {
        super(Wallet.class, forVariable(variable));
    }

    public QWallet(Path<? extends Wallet> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWallet(PathMetadata metadata) {
        super(Wallet.class, metadata);
    }

}

