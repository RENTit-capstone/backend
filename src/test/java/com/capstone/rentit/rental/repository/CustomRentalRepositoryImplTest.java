package com.capstone.rentit.rental.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class CustomRentalRepositoryImplTest {

    @Autowired
    private RentalRepository rentalRepository;

    // 테스트용 상수
    private static final Long USER_ID = 1L;
    private Rental requestedByUser;
    private Rental approvedForUser;
    private Rental otherRental;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2025, 4, 28, 0, 0);

        requestedByUser = createRental(USER_ID, 99L, 100L, RentalStatusEnum.REQUESTED);
        approvedForUser  = createRental(42L, USER_ID, 100L, RentalStatusEnum.APPROVED);
        otherRental      = createRental(8L,  9L,    200L, RentalStatusEnum.REQUESTED);

        rentalRepository.saveAll(List.of(requestedByUser, approvedForUser, otherRental));
    }

    @Test
    @DisplayName("상태 필터가 비어 있으면, 해당 유저의 모든 대여를 반환한다")
    void whenStatusesEmpty_thenReturnAllRentalsForUser() {
        //given, when
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                USER_ID,
                Collections.emptyList(),
                Pageable.unpaged()
        );
        List<Rental> results = page.getContent();

        //then
        assertThat(results)
                .hasSize(2)
                .containsExactlyInAnyOrder(requestedByUser, approvedForUser);
    }

    @Test
    @DisplayName("특정 상태만 필터링하면, 해당 상태의 대여만 반환한다")
    void whenStatusesProvided_thenReturnOnlyFilteredRentals() {
        //given, when
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                USER_ID,
                List.of(RentalStatusEnum.APPROVED),
                Pageable.unpaged()
        );
        List<Rental> results = page.getContent();

        //then
        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(Rental::getStatus)
                .isEqualTo(RentalStatusEnum.APPROVED);
    }

    @Test
    @DisplayName("해당 유저의 대여가 하나도 없으면, 빈 리스트를 반환한다")
    void whenUserHasNoRentals_thenReturnEmptyList() {
        //given, when
        Long otherUserId = 9999L;
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                otherUserId,
                null,
                Pageable.unpaged()
        );
        List<Rental> results = page.getContent();

        //then
        assertThat(results).isEmpty();
    }

    // 헬퍼: 빌더 패턴으로 Rental 객체 생성
    private Rental createRental(Long ownerId, Long renterId, Long itemId, RentalStatusEnum status) {
        return Rental.builder()
                .ownerId(ownerId)
                .renterId(renterId)
                .itemId(itemId)
                .status(status)
                .requestDate(baseTime)
                .startDate(baseTime.plusDays(1))
                .dueDate(baseTime.plusDays(7))
                .build();
    }
}