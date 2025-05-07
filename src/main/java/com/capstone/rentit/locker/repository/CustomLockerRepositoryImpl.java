package com.capstone.rentit.locker.repository;

import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.domain.QLocker;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomLockerRepositoryImpl implements CustomLockerRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Locker> search(LockerSearchForm form) {
        QLocker locker = QLocker.locker;
        BooleanBuilder builder = new BooleanBuilder();

        if (form.getUniversity() != null && !form.getUniversity().isBlank()) {
            builder.and(locker.university.containsIgnoreCase(form.getUniversity()));
        }
        if (form.getAvailable() != null) {
            builder.and(locker.available.eq(form.getAvailable()));
        }

        return queryFactory
                .selectFrom(locker)
                .where(builder)
                .orderBy(locker.lockerId.asc())
                .fetch();
    }
}
