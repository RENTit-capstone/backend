package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class ItemRepositoryTest {

    @Autowired
    private EntityManager em;

    private CustomItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long defaultOwnerId;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        itemRepository = new CustomItemRepositoryImpl(queryFactory);

        Student common = Student.builder()
                .email("common@example.com")
                .password("pwd")
                .name("commonUser")
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .studentId("C000")
                .university("CommonUniv")
                .nickname("commonNick")
                .phone("010-0000-0000")
                .build();
        common = memberRepository.save(common);
        defaultOwnerId = common.getMemberId();
    }

    @Test
    @DisplayName("1. unpaged + 모든 검색 조건 null → 모든 Item을 createdAt DESC 순으로 반환")
    void whenUnpagedAndAllCriteriaNull_thenReturnAllSortedDesc() {
        // given: 세 가지 다른 createdAt 타임스탬프를 가진 아이템
        Item i1 = saveItem("A", "foo", ItemStatusEnum.AVAILABLE,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(5),
                100, LocalDateTime.now().minusDays(3), defaultOwnerId);
        Item i2 = saveItem("B", "bar", ItemStatusEnum.OUT,
                LocalDateTime.now().minusDays(4), LocalDateTime.now().plusDays(4),
                200, LocalDateTime.now().minusDays(2), defaultOwnerId);
        Item i3 = saveItem("C", "baz", ItemStatusEnum.AVAILABLE,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().plusDays(3),
                300, LocalDateTime.now().minusDays(1), defaultOwnerId);
        em.flush();

        // when
        ItemSearchForm form = new ItemSearchForm(); // 모든 필드 null
        Page<Item> result = itemRepository.search(form, Pageable.unpaged());

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        // DESC(createdAt): i3, i2, i1 순
        assertThat(result.getContent()).containsExactly(i3, i2, i1);
    }

    @Test
    @DisplayName("2. paged + keyword & status 필터 → name/description에 keyword 포함 & status 일치 아이템만 반환")
    void whenPagedAndKeywordAndStatus_thenFilterByKeywordAndStatus() {
        // given
        Item first = saveItem("Alpha", "first", ItemStatusEnum.AVAILABLE,
                null, null, 100, LocalDateTime.now().minusDays(2), defaultOwnerId);
        Item match = saveItem("BetaAlpha", "second", ItemStatusEnum.AVAILABLE,
                null, null, 150, LocalDateTime.now().minusDays(1), defaultOwnerId);
        saveItem("Gamma", "third", ItemStatusEnum.OUT,
                null, null, 200, LocalDateTime.now(), defaultOwnerId);
        em.flush();

        // when
        ItemSearchForm form = new ItemSearchForm();
        form.setKeyword("alpha");
        form.setStatus(ItemStatusEnum.AVAILABLE);

        Pageable pg = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Item> page = itemRepository.search(form, pg);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        // keyword "alpha" 매칭 + status AVAILABLE → 두 아이템
        assertThat(page.getContent()).containsExactly(match, first);
    }

    @Test
    @DisplayName("3. paged + 날짜·가격 범위 필터 + ASC 정렬 → 조건에 맞는 아이템을 createdAt ASC 순으로 반환")
    void whenPagedAndDateAndPriceAndAscSort_thenFilterAndSortAsc() {
        LocalDateTime now = LocalDateTime.now();

        // given: 범위 내 2개 아이템, 1개는 범위 밖
        Item outOfRange = saveItem("X", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(10), now.minusDays(9),
                50, now.minusDays(4), defaultOwnerId);

        Item i1 = saveItem("Y", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(3), now.plusDays(1),
                200, now.minusDays(3), defaultOwnerId);
        Item i2 = saveItem("Z", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(2), now.plusDays(2),
                220, now.minusDays(1), defaultOwnerId);
        em.flush();

        // filter: startDate ≥ now−5, endDate ≤ now+3, price 150~250
        ItemSearchForm form = new ItemSearchForm();
        form.setStartDate(now.minusDays(5));
        form.setEndDate(now.plusDays(3));
        form.setMinPrice(150);
        form.setMaxPrice(250);

        Pageable pg = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        Page<Item> page = itemRepository.search(form, pg);

        // then: outOfRange는 제외, i1·i2만 포함, ASC(createdAt): i1, i2 순
        assertThat(page.getTotalElements()).isEqualTo(2);
        List<Item> content = page.getContent();
        assertThat(content).containsExactly(i1, i2);
    }

    @Test
    @DisplayName("여러 역할(STUDENT, COMPANY, STUDENT_COUNCIL)로 검색 시 해당 역할 소유 아이템만 조회")
    void search_ByStudentCompanyAndCouncilRoles_ReturnsAllTwo() {
        // given: STUDENT, COMPANY, STUDENT_COUNCIL 멤버 저장
        Student student = Student.builder()
                .email("stu2@example.com").password("pwd").name("student2")
                .role(MemberRoleEnum.STUDENT).locked(false).createdAt(LocalDate.now())
                .studentId("S456").university("TestUniv").nickname("stuNick2").phone("010-1111-2222")
                .build();
        student = (Student) memberRepository.save(student);

        Company company = Company.builder()
                .email("comp2@example.com").password("pwd").name("company2").nickname("company2")
                .role(MemberRoleEnum.COMPANY).locked(false).createdAt(LocalDate.now())
                .companyName("CompName2")
                .build();
        company = (Company) memberRepository.save(company);

        StudentCouncilMember council = StudentCouncilMember.builder()
                .email("council@example.com").password("pwd").name("council").nickname("council")
                .role(MemberRoleEnum.COUNCIL).locked(false).createdAt(LocalDate.now())
                .build();
        council = (StudentCouncilMember) memberRepository.save(council);

        // 각 멤버 소유 아이템 생성
        LocalDateTime now = LocalDateTime.now();
        Item itemByStudent = saveItem("STUDENT_ITEM", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(3), now.plusDays(1),
                200, now.minusDays(3), student.getMemberId());
        Item itemByCompany = saveItem("COUNCIL_ITEM", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(3), now.plusDays(1),
                200, now.minusDays(3), company.getMemberId());
        Item itemByCouncil = saveItem("COMPANY_ITEM", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(3), now.plusDays(1),
                200, now.minusDays(3), council.getMemberId());
        em.flush();

        // when: STUDENT, COMPANY, STUDENT_COUNCIL 세 역할로 검색
        ItemSearchForm form = new ItemSearchForm();
        form.setOwnerRoles(Arrays.asList(
                MemberRoleEnum.STUDENT,
                MemberRoleEnum.COMPANY
        ));
        Pageable pg = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
        Page<Item> result = itemRepository.search(form, pg);

        // then: 저장한 학생, 기업 아이템이 조회되어야 함
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Item::getOwnerId)
                .containsExactlyInAnyOrder(
                        student.getMemberId(),
                        company.getMemberId()
                );
    }

    // — 헬퍼 메서드: Item 생성 & persist —
    private Item saveItem(String name,
                          String description,
                          ItemStatusEnum status,
                          LocalDateTime startDate,
                          LocalDateTime endDate,
                          Integer price,
                          LocalDateTime createdAt,
                          Long ownerId) {
        Item it = Item.builder()
                .name(name)
                .description(description)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .price(price)
                .createdAt(createdAt)
                .damagedPolicy("NONE")
                .returnPolicy("NONE")
                .ownerId(ownerId)
                .build();
        em.persist(it);
        return it;
    }
}