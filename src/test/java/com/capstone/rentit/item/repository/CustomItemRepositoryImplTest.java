package com.capstone.rentit.item.repository;

import com.capstone.rentit.common.ItemStatusEnum;
import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemSearchForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class ItemRepositoryIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    private Item cheapItem;
    private Item expensiveItem;

    @BeforeEach
    void setUp() {
        // 가격 1,000
        cheapItem = Item.builder()
                .ownerId(1L)
                .name("Cheap")
                .itemImg("cheap.jpg")
                .description("저렴한 물품")
                .categoryId(1L)
                .price(1000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("DP")
                .returnPolicy("RP")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        // 가격 5,000
        expensiveItem = Item.builder()
                .ownerId(2L)
                .name("Expensive")
                .itemImg("exp.jpg")
                .description("비싼 물품")
                .categoryId(2L)
                .price(5000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("DP")
                .returnPolicy("RP")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        itemRepository.saveAll(List.of(cheapItem, expensiveItem));
    }

    @DisplayName("조건 없이 호출하면, 저장된 모든 아이템을 반환")
    @Test
    void search_whenNoCriteria_thenReturnAll() {
        ItemSearchForm form = new ItemSearchForm();  // 모든 필드 null
        List<Item> result = itemRepository.search(form);

        assertThat(result).hasSize(2)
                .extracting(Item::getName)
                .containsExactlyInAnyOrder("Cheap", "Expensive");
    }

    @DisplayName("최대 가격 설정 시, 가격 ≤ maxPrice 인 아이템만 반환")
    @Test
    void search_whenMaxPrice_thenFilterByMax() {
        ItemSearchForm form = new ItemSearchForm();
        form.setMaxPrice(2000);  // 2,000원 이하

        List<Item> result = itemRepository.search(form);

        assertThat(result).hasSize(1)
                .first()
                .extracting(Item::getName)
                .isEqualTo("Cheap");
    }

    @DisplayName("최소 가격 설정 시, 가격 ≥ minPrice 인 아이템만 반환")
    @Test
    void search_whenMinPrice_thenFilterByMin() {
        ItemSearchForm form = new ItemSearchForm();
        form.setMinPrice(2000);  // 2,000원 이상

        List<Item> result = itemRepository.search(form);

        assertThat(result).hasSize(1)
                .first()
                .extracting(Item::getName)
                .isEqualTo("Expensive");
    }
}