package com.capstone.rentit.rental.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class CustomRentalRepositoryImplTest {

    @Autowired
    private EntityManager em;

    private CustomRentalRepositoryImpl customRepo;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        customRepo = new CustomRentalRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("1. unpaged + 빈 status → 해당 user의 모든 Rental을 requestDate DESC 순으로 반환")
    void whenUnpagedAndEmptyStatuses_thenReturnAllForUser() {
        // given
        Rental r1 = saveRental(1L, 2L, RentalStatusEnum.REQUESTED, LocalDateTime.now().minusDays(2));
        Rental r2 = saveRental(3L, 1L, RentalStatusEnum.APPROVED, LocalDateTime.now().minusDays(1));
        Rental r3 = saveRental(4L, 5L, RentalStatusEnum.COMPLETED, LocalDateTime.now());
        em.flush();

        // when
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(1L, Collections.emptyList(), Pageable.unpaged());

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        // 가장 최신 requestDate 순: r2, r1
        assertThat(page.getContent()).containsExactly(r2, r1);
    }

    @Test
    @DisplayName("2. paged + 특정 statuses → 필터링 후 requestDate DESC 페이징")
    void whenPagedAndSpecificStatuses_thenFilterByStatusAndPage() {
        // given
        // userId = 1 인 것 중 APPROVED, RETURNED만 골라본다
        saveRental(1L, 2L, RentalStatusEnum.REQUESTED, LocalDateTime.now().minusDays(3));
        Rental r2 = saveRental(1L, 3L, RentalStatusEnum.APPROVED, LocalDateTime.now().minusDays(2));
        Rental r3 = saveRental(4L, 1L, RentalStatusEnum.COMPLETED, LocalDateTime.now().minusDays(1));
        saveRental(5L, 6L, RentalStatusEnum.APPROVED, LocalDateTime.now());
        em.flush();

        Pageable pg = PageRequest.of(0, 10, Sort.by("requestDate").descending());

        // when
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(
                1L,
                Arrays.asList(RentalStatusEnum.APPROVED, RentalStatusEnum.COMPLETED),
                pg
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).containsExactly(r3, r2);
    }

    @Test
    @DisplayName("3. paged + asc 정렬 → requestDate ASC 순으로 반환")
    void whenPagedWithAscSort_thenOrderByRequestDateAsc() {
        // given
        Rental r1 = saveRental(1L, 2L, RentalStatusEnum.APPROVED, LocalDateTime.now().minusDays(2));
        Rental r2 = saveRental(3L, 1L, RentalStatusEnum.APPROVED, LocalDateTime.now().minusDays(1));
        Rental r3 = saveRental(1L, 4L, RentalStatusEnum.APPROVED, LocalDateTime.now());
        em.flush();

        Pageable pg = PageRequest.of(0, 10, Sort.by("requestDate").ascending());

        // when
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(
                1L,
                Collections.singletonList(RentalStatusEnum.APPROVED),
                pg
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(r1, r2, r3);
    }

    // — 헬퍼 메서드: 중복 코드 방지 —
    private Rental saveRental(Long ownerId,
                              Long renterId,
                              RentalStatusEnum status,
                              LocalDateTime requestDate) {
        Rental r = Rental.builder()
                .ownerId(ownerId)
                .renterId(renterId)
                .itemId(100L)
                .status(status)
                .requestDate(requestDate)
                .dueDate(requestDate.plusDays(7))
                .startDate(requestDate.plusDays(1))
                .build();
        em.persist(r);
        return r;
    }
}