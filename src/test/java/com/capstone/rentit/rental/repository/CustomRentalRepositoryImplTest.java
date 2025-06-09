package com.capstone.rentit.rental.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.MemberRoleEnum;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class CustomRentalRepositoryImplTest {

    @Autowired
    private EntityManager em;

    private CustomRentalRepositoryImpl rentalRepository;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        rentalRepository = new CustomRentalRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("1. unpaged + 빈 status → 해당 user의 모든 Rental을 requestDate DESC 순으로 반환")
    void whenUnpagedAndEmptyStatuses_thenReturnAllForUser() {
        //given
        Member user   = saveMember("user");
        Member other1 = saveMember("other1");
        Member other2 = saveMember("other2");

        Rental r1 = saveRental(user,   other1, RentalStatusEnum.REQUESTED,
                LocalDateTime.now().minusDays(2)); // user = owner
        Rental r2 = saveRental(other1, user,   RentalStatusEnum.APPROVED,
                LocalDateTime.now().minusDays(1)); // user = renter
        Rental r3 = saveRental(other2, other1, RentalStatusEnum.COMPLETED,
                LocalDateTime.now());              // user 와 무관
        em.flush();

        // ─ when ─
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                user.getMemberId(), Collections.emptyList(), Pageable.unpaged());

        // ─ then ─
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).containsExactly(r2, r1); // 최신순
    }

    @Test
    @DisplayName("2. paged + 특정 statuses → 필터링 후 requestDate DESC 페이징")
    void whenPagedAndSpecificStatuses_thenFilterByStatusAndPage() {
        // given
        Member user   = saveMember("user");
        Member other1 = saveMember("other1");
        Member other2 = saveMember("other2");

        saveRental(user,   other1, RentalStatusEnum.REQUESTED,
                LocalDateTime.now().minusDays(3));
        Rental r2 = saveRental(user,   other2, RentalStatusEnum.APPROVED,
                LocalDateTime.now().minusDays(2));
        Rental r3 = saveRental(other1, user,   RentalStatusEnum.COMPLETED,
                LocalDateTime.now().minusDays(1));
        saveRental(other2, other1, RentalStatusEnum.APPROVED,
                LocalDateTime.now());
        em.flush();

        Pageable pg = PageRequest.of(0, 10,
                Sort.by("requestDate").descending());

        // ─ when ─
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                user.getMemberId(),
                Arrays.asList(RentalStatusEnum.APPROVED, RentalStatusEnum.COMPLETED),
                pg);

        // ─ then ─
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).containsExactly(r3, r2);
    }

    @Test
    @DisplayName("3. paged + asc 정렬 → requestDate ASC 순으로 반환")
    void whenPagedWithAscSort_thenOrderByRequestDateAsc() {
        // given
        Member user = saveMember("user");
        Member o1   = saveMember("o1");

        Rental r1 = saveRental(user, o1, RentalStatusEnum.APPROVED,
                LocalDateTime.now().minusDays(2));
        Rental r2 = saveRental(o1,   user, RentalStatusEnum.APPROVED,
                LocalDateTime.now().minusDays(1));
        Rental r3 = saveRental(user, o1,  RentalStatusEnum.APPROVED,
                LocalDateTime.now());
        em.flush();

        Pageable pg = PageRequest.of(0, 10,
                Sort.by("requestDate").ascending());

        // ─ when ─
        Page<Rental> page = rentalRepository.findAllByUserIdAndStatuses(
                user.getMemberId(),
                Collections.singletonList(RentalStatusEnum.APPROVED),
                pg);

        // ─ then ─
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(r1, r2, r3);
    }

    @Test
    @DisplayName("PICK_UP_BY_RENTER: LEFT_IN_LOCKER 상태인 renterId 의 렌탈만 반환")
    void findEligibleRentals_pickUpByRenter() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Member ownerA = saveMember("ownerA");
        Member ownerB = saveMember("ownerB");
        Member renter = saveMember("renter");

        Rental r1 = saveRental(ownerA, renter, RentalStatusEnum.LEFT_IN_LOCKER, now.minusHours(4));
        Rental r2 = saveRental(ownerB, renter, RentalStatusEnum.LEFT_IN_LOCKER, now.minusHours(1));

        // 제외 대상
        saveRental(ownerA, saveMember("otherRenter"), RentalStatusEnum.LEFT_IN_LOCKER, now.minusHours(2));
        saveRental(ownerA, renter, RentalStatusEnum.PICKED_UP, now.minusHours(3));

        em.flush();
        em.clear();

        // Act
        List<Rental> result =
                rentalRepository.findEligibleRentals(renter.getMemberId(), RentalLockerAction.PICK_UP_BY_RENTER);

        // Assert
        assertThat(result)
                .hasSize(2)
                .extracting(Rental::getRentalId)
                .containsExactly(r2.getRentalId(), r1.getRentalId());
    }

    @Test
    @DisplayName("RETURN_BY_RENTER: PICKED_UP 상태인 renterId 의 렌탈만 반환")
    void findEligibleRentals_returnByRenter() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Member owner = saveMember("ownerX");
        Member renter = saveMember("renterX");

        Rental matching = saveRental(owner, renter, RentalStatusEnum.PICKED_UP, now.minusDays(1));

        // 제외 대상
        saveRental(owner, saveMember("otherRenter"), RentalStatusEnum.PICKED_UP, now.minusDays(2));
        saveRental(owner, renter, RentalStatusEnum.LEFT_IN_LOCKER, now.minusHours(5));

        em.flush();
        em.clear();

        // Act
        List<Rental> result =
                rentalRepository.findEligibleRentals(renter.getMemberId(), RentalLockerAction.RETURN_BY_RENTER);

        // Assert
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Rental::getRentalId)
                .isEqualTo(matching.getRentalId());
    }

    @Test
    @DisplayName("RETRIEVE_BY_OWNER: RETURNED_TO_LOCKER 상태인 ownerId 의 렌탈만 반환")
    void findEligibleRentals_retrieveByOwner() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Member owner = saveMember("ownerY");
        Member renter = saveMember("renterY");

        Rental matching = saveRental(owner, renter, RentalStatusEnum.RETURNED_TO_LOCKER, now.minusDays(1));

        // 제외 대상
        saveRental(saveMember("otherOwner"), renter, RentalStatusEnum.RETURNED_TO_LOCKER, now.minusHours(3));
        saveRental(owner, renter, RentalStatusEnum.PICKED_UP, now.minusHours(2));

        em.flush();
        em.clear();

        // Act
        List<Rental> result =
                rentalRepository.findEligibleRentals(owner.getMemberId(), RentalLockerAction.RETRIEVE_BY_OWNER);

        // Assert
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Rental::getRentalId)
                .isEqualTo(matching.getRentalId());
    }

    @Test
    @DisplayName("4. (findAllByStatuses) unpaged + 빈 status → 모든 Rental을 requestDate DESC 순으로 반환")
    void whenUnpagedAndEmptyStatuses_findAllByStatuses_returnsAllSortedByRequestDateDesc() {
        // given: 서로 다른 requestDate를 가진 3개의 Rental을 저장
        LocalDateTime now = LocalDateTime.now();
        Member m1 = saveMember("user1");
        Member m2 = saveMember("user2");

        Rental r1 = saveRental(m1, m2, RentalStatusEnum.REQUESTED, now.minusDays(2));
        Rental r2 = saveRental(m2, m1, RentalStatusEnum.APPROVED, now.minusDays(1));
        Rental r3 = saveRental(m1, m2, RentalStatusEnum.COMPLETED, now);
        em.flush();

        // when
        Page<Rental> page = rentalRepository.findAllByStatuses(
                Collections.emptyList(),
                Pageable.unpaged()
        );

        // then: 총 3개, requestDate 최신순 (r3, r2, r1)
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(r3, r2, r1);
    }

    @Test
    @DisplayName("5. (findAllByStatuses) paged + 특정 statuses → 상태 필터 후 requestDate DESC 페이징 반환")
    void whenPagedAndSpecificStatuses_findAllByStatuses_filtersByStatusAndPages() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Member owner = saveMember("ownerA");
        Member renter = saveMember("renterA");

        // REQUESTED, APPROVED, COMPLETED 상태의 Rental을 각각 하나씩 생성
        saveRental(owner, renter, RentalStatusEnum.REQUESTED, now.minusDays(3));
        Rental r2 = saveRental(owner, renter, RentalStatusEnum.APPROVED, now.minusDays(2));
        Rental r3 = saveRental(owner, renter, RentalStatusEnum.COMPLETED, now.minusDays(1));
        Rental r4 = saveRental(owner, renter, RentalStatusEnum.APPROVED, now);

        em.flush();

        // "APPROVED" 또는 "COMPLETED"만 필터링, requestDate 내림차순 정렬
        Pageable pageable = PageRequest.of(0, 10, Sort.by("requestDate").descending());

        // when
        Page<Rental> page = rentalRepository.findAllByStatuses(
                Arrays.asList(RentalStatusEnum.APPROVED, RentalStatusEnum.COMPLETED),
                pageable
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);

        // 실제 content 순서: r4 (APPROVED@now), r3 (COMPLETED@now.minusDays(1)), r2 (APPROVED@now.minusDays(2))
        List<Rental> content = page.getContent();
        assertThat(content)
                .extracting(Rental::getStatus)
                .containsExactly(
                        RentalStatusEnum.APPROVED,
                        RentalStatusEnum.COMPLETED,
                        RentalStatusEnum.APPROVED
                );

        // 추가로 requestDate가 내림차순인지 확인
        assertThat(content)
                .extracting(Rental::getRequestDate)
                .isSortedAccordingTo((d1, d2) -> d2.compareTo(d1));
    }

    @Test
    @DisplayName("6. (findAllByStatuses) paged + asc 정렬 by dueDate → dueDate ASC 순으로 반환")
    void whenPagedAndSortByDueDateAsc_findAllByStatuses_ordersByDueDateAsc() {
        // given: 세 개의 Rental을 서로 다른 requestDate → dueDate도 서로 달라짐
        LocalDateTime now = LocalDateTime.now();
        Member owner = saveMember("ownerB");
        Member renter = saveMember("renterB");

        // r1.dueDate = now.minusDays(2) + 7일 = now.plusDays(5)
        Rental r1 = saveRental(owner, renter, RentalStatusEnum.APPROVED, now.minusDays(2));
        // r2.dueDate = now.minusDays(1) + 7일 = now.plusDays(6)
        Rental r2 = saveRental(owner, renter, RentalStatusEnum.APPROVED, now.minusDays(1));
        // r3.dueDate = now + 7일 = now.plusDays(7)
        Rental r3 = saveRental(owner, renter, RentalStatusEnum.APPROVED, now);

        em.flush();

        // dueDate 기준 오름차순 정렬
        Pageable pageable = PageRequest.of(0, 10, Sort.by("dueDate").ascending());

        // when
        Page<Rental> page = rentalRepository.findAllByStatuses(
                Collections.singletonList(RentalStatusEnum.APPROVED),
                pageable
        );

        // then: 모두 APPROVED 상태 → 총 3개, dueDate ASC (r1, r2, r3)
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .containsExactly(r1, r2, r3);
    }

    @Test
    @DisplayName("7. (findAllByStatuses) paged + sort by startDate DESC → startDate DESC 순으로 반환")
    void whenPagedAndSortByStartDateDesc_findAllByStatuses_ordersByStartDateDesc() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Member owner = saveMember("ownerC");
        Member renter = saveMember("renterC");

        // saveRental 설정에 따라 startDate = requestDate + 1일
        Rental r1 = saveRental(owner, renter, RentalStatusEnum.REQUESTED, now.minusDays(3)); // startDate = now.minusDays(2)
        Rental r2 = saveRental(owner, renter, RentalStatusEnum.REQUESTED, now.minusDays(2)); // startDate = now.minusDays(1)
        Rental r3 = saveRental(owner, renter, RentalStatusEnum.REQUESTED, now.minusDays(1)); // startDate = now

        em.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").descending());

        // when
        Page<Rental> page = rentalRepository.findAllByStatuses(
                Collections.singletonList(RentalStatusEnum.REQUESTED),
                pageable
        );

        // then: 총 3개, startDate DESC (r3, r2, r1)
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly(r3, r2, r1);
    }

    @Test
    @DisplayName("8. findByIdWithItem: 유효한 rentalId로 Rental과 Item을 fetch join하여 가져온다")
    void findByIdWithItem_returnsRentalWithItem() {
        // Given
        Member owner = saveMember("testOwner");
        Member renter = saveMember("testRenter");
        Item item = saveItem(owner, "Test Item Name");
        Rental rental = saveRental(owner, renter, item, RentalStatusEnum.REQUESTED, LocalDateTime.now());
        em.flush(); // 변경 사항을 DB에 반영
        em.clear(); // 영속성 컨텍스트 초기화하여 fetch join 효과를 확실히 테스트

        // When
        Optional<Rental> foundRentalOptional = rentalRepository.findByIdWithItem(rental.getRentalId());

        // Then
        assertThat(foundRentalOptional).isPresent();
        Rental foundRental = foundRentalOptional.get();

        // Rental ID가 일치하는지 확인
        assertThat(foundRental.getRentalId()).isEqualTo(rental.getRentalId());

        // Item이 null이 아닌지, 그리고 fetch join으로 잘 가져와졌는지 확인
        assertThat(foundRental.getItem()).isNotNull();
        assertThat(foundRental.getItem().getItemId()).isEqualTo(item.getItemId());
        assertThat(foundRental.getItem().getName()).isEqualTo("Test Item Name");
    }

    @Test
    @DisplayName("9. findByIdWithItem: 존재하지 않는 rentalId로 조회 시 Optional.empty()를 반환한다")
    void findByIdWithItem_returnsEmptyForNonExistingId() {
        // Given
        Long nonExistingRentalId = 999L; // 존재하지 않는 ID

        // When
        Optional<Rental> foundRentalOptional = rentalRepository.findByIdWithItem(nonExistingRentalId);

        // Then
        assertThat(foundRentalOptional).isEmpty(); // Optional이 비어있음을 확인
    }

    // — 헬퍼 메서드: 중복 코드 방지 —
    private Member saveMember(String name) {
        Member m = Student.builder()                 // STUDENT 서브클래스 예시
                .email(name + "@test.com")
                .password("pwd")
                .name(name)
                .nickname("Nick" + name)
                .role(MemberRoleEnum.STUDENT)
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();
        em.persist(m);
        return m;
    }

    private Item saveItem(Member owner, String itemName) {
        Item it = Item.builder()
                .ownerId(owner.getMemberId())
                .name(itemName)
                .description("dummy")
                .price(1000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp")
                .returnPolicy("rp")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        em.persist(it);
        return it;
    }

    private Rental saveRental(Member owner,
                              Member renter,
                              RentalStatusEnum status,
                              LocalDateTime requestDate) {

        Item item = saveItem(owner, "item-" + requestDate.toString());

        Rental r = Rental.builder()
                .ownerId(owner.getMemberId())
                .renterId(renter.getMemberId())
                .itemId(item.getItemId())
                .status(status)
                .requestDate(requestDate)
                .startDate(requestDate.plusDays(1))
                .dueDate(requestDate.plusDays(7))
                .build();
        em.persist(r);
        return r;
    }
    private Rental saveRental(Member owner,
                              Member renter,
                              Item item,
                              RentalStatusEnum status,
                              LocalDateTime requestDate) {

        Rental r = Rental.builder()
                .ownerId(owner.getMemberId())
                .renterId(renter.getMemberId())
                .itemId(item.getItemId())
                .status(status)
                .requestDate(requestDate)
                .startDate(requestDate.plusDays(1))
                .dueDate(requestDate.plusDays(7))
                .build();
        em.persist(r);
        return r;
    }
}