package com.capstone.rentit.rental.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
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
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(
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
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(
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
        Page<Rental> page = customRepo.findAllByUserIdAndStatuses(
                user.getMemberId(),
                Collections.singletonList(RentalStatusEnum.APPROVED),
                pg);

        // ─ then ─
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

    private Member saveMember(String name) {
        Member m = Student.builder()                 // STUDENT 서브클래스 예시
                .email(name + "@test.com")
                .password("pwd")                    // 필수 컬럼 최소값
                .name(name)
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
}