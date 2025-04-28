package com.capstone.rentit.item.repository;

import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class ItemRepositoryTest {

    @Autowired
    private EntityManager em;

    private CustomItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        itemRepository = new CustomItemRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("1. unpaged + 모든 검색 조건 null → 모든 Item을 createdAt DESC 순으로 반환")
    void whenUnpagedAndAllCriteriaNull_thenReturnAllSortedDesc() {
        // given: 세 가지 다른 createdAt 타임스탬프를 가진 아이템
        Item i1 = saveItem("A", "foo", ItemStatusEnum.AVAILABLE,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(5),
                100, LocalDateTime.now().minusDays(3));
        Item i2 = saveItem("B", "bar", ItemStatusEnum.OUT,
                LocalDateTime.now().minusDays(4), LocalDateTime.now().plusDays(4),
                200, LocalDateTime.now().minusDays(2));
        Item i3 = saveItem("C", "baz", ItemStatusEnum.AVAILABLE,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().plusDays(3),
                300, LocalDateTime.now().minusDays(1));
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
                null, null, 100, LocalDateTime.now().minusDays(2));
        Item match = saveItem("BetaAlpha", "second", ItemStatusEnum.AVAILABLE,
                null, null, 150, LocalDateTime.now().minusDays(1));
        saveItem("Gamma", "third", ItemStatusEnum.OUT,
                null, null, 200, LocalDateTime.now());
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
                50, now.minusDays(4));

        Item i1 = saveItem("Y", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(3), now.plusDays(1),
                200, now.minusDays(3));
        Item i2 = saveItem("Z", "", ItemStatusEnum.AVAILABLE,
                now.minusDays(2), now.plusDays(2),
                220, now.minusDays(1));
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

    // — 헬퍼 메서드: Item 생성 & persist —
    private Item saveItem(String name,
                          String description,
                          ItemStatusEnum status,
                          LocalDateTime startDate,
                          LocalDateTime endDate,
                          Integer price,
                          LocalDateTime createdAt) {
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
                .ownerId(1L)
                .build();
        em.persist(it);
        return it;
    }
}