package com.capstone.rentit.inquiry.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QInquiry is a Querydsl query type for Inquiry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInquiry extends EntityPathBase<Inquiry> {

    private static final long serialVersionUID = -693851968L;

    public static final QInquiry inquiry = new QInquiry("inquiry");

    public final StringPath answer = createString("answer");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> inquiryId = createNumber("inquiryId", Long.class);

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    public final BooleanPath processed = createBoolean("processed");

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    public final EnumPath<com.capstone.rentit.inquiry.type.InquiryType> type = createEnum("type", com.capstone.rentit.inquiry.type.InquiryType.class);

    public QInquiry(String variable) {
        super(Inquiry.class, forVariable(variable));
    }

    public QInquiry(Path<? extends Inquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInquiry(PathMetadata metadata) {
        super(Inquiry.class, metadata);
    }

}

