package com.capstone.rentit.member.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.CustomItemRepositoryImpl;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class CustomMemberRepositoryTest {

    @Autowired
    private CustomMemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Querydsl 을 사용하기 위해 테스트용 JPAQueryFactory Bean 등록
     */
    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        memberRepository = new CustomMemberRepositoryImpl(queryFactory);
    }


    @Test
    @DisplayName("아무 연관관계가 없으면 Optional.empty 가 아니다, 컬렉션은 비어 있다")
    void findProfileWithAll_whenNoAssociations_thenReturnMemberWithEmptyCollections() {
        // given
        Member m = Student.builder()
                .email("no@assoc.com").name("NoAssoc").nickname("nickNoAssoc")
                .password("123").role(MemberRoleEnum.STUDENT)
                .university("univ").studentId("20250001")
                .build();
        em.persist(m);
        em.flush();
        em.clear();

        // when
        Optional<Member> opt = memberRepository.findProfileWithAll(m.getMemberId());

        // then
        assertThat(opt).isPresent();
        Member fetched = opt.get();
        assertThat(fetched.getMemberId()).isEqualTo(m.getMemberId());
        assertThat(fetched.getItems()).isEmpty();
        assertThat(fetched.getOwnedRentals()).isEmpty();
        assertThat(fetched.getRentedRentals()).isEmpty();
    }

    @Test
    @DisplayName("아이템·대여정보가 있으면 모두 fetchJoin 되어 돌아온다")
    void findProfileWithAll_whenHasAssociations_thenReturnMemberWithAllFetched() {
        // given
        Member owner = Student.builder()
                .email("owner@example.com").name("Owner").nickname("NickOwner")
                .password("123").role(MemberRoleEnum.STUDENT)
                .university("univ").studentId("20250002")
                .build();
        em.persist(owner);

        // 내가 등록한 아이템
        Item item = Item.builder()
                .name("Drill").ownerId(owner.getMemberId()).owner(owner)
                .imageKeys(List.of("image_url")).description("desc1")
                .status(ItemStatusEnum.AVAILABLE)
                .returnPolicy("return pol").damagedPolicy("damage pol").build();
        em.persist(item);

        // 내가 대여자로서 빌린 대여 (다른 사람 소유 아이템 가정)
        Member renter = Student.builder()
                .email("renter@example.com").name("Renter").nickname("NickRenter")
                .password("123").role(MemberRoleEnum.STUDENT)
                .university("univ").studentId("20250003")
                .build();
        em.persist(renter);

        // 내가 소유자로서 빌려준 대여
        Rental ownedRental = Rental.builder()
                .itemId(item.getItemId()).ownerId(owner.getMemberId()).ownerMember(owner)
                .renterId(renter.getMemberId()).renterMember(renter)
                .requestDate(LocalDateTime.now()).startDate(LocalDateTime.now())
                .status(RentalStatusEnum.APPROVED).dueDate(LocalDateTime.now())
                .build();
        em.persist(ownedRental);

        Item other = Item.builder()
                .name("Saw").ownerId(renter.getMemberId()).owner(renter)
                .imageKeys(List.of("image_url")).description("desc2")
                .status(ItemStatusEnum.AVAILABLE)
                .returnPolicy("return pol").damagedPolicy("damage pol").build();
        em.persist(other);

        Rental rentedRental =  Rental.builder()
                .itemId(other.getItemId()).ownerId(renter.getMemberId()).ownerMember(renter)
                .renterId(owner.getMemberId()).renterMember(owner)
                .requestDate(LocalDateTime.now()).startDate(LocalDateTime.now())
                .status(RentalStatusEnum.APPROVED).dueDate(LocalDateTime.now())
                .build();
        em.persist(rentedRental);

        em.flush();
        em.clear();

        // when
        Optional<Member> opt = memberRepository.findProfileWithAll(owner.getMemberId());

        // then
        assertThat(opt).isPresent();
        Member fetched = opt.get();

        // 기본 Member 필드
        assertThat(fetched.getEmail()).isEqualTo("owner@example.com");
        // 연관된 컬렉션 사이즈 검증
        assertThat(fetched.getItems())
                .extracting(Item::getName)
                .containsExactly("Drill");

        assertThat(fetched.getOwnedRentals())
                .extracting(r -> r.getItem().getName())
                .containsExactly("Drill");

        assertThat(fetched.getRentedRentals())
                .extracting(r -> r.getItem().getName())
                .containsExactly("Saw");
    }
}