package com.capstone.rentit.item.service;

import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.exception.ItemUnauthorizedException;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private ItemCreateForm createForm;
    private ItemUpdateForm updateForm;
    private Item sampleItem;
    private MemberDto ownerMember;
    private MemberDto otherMember;
    private Pageable pageable;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private MemberDetailsService memberDetailsService;

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 준비
        createForm = new ItemCreateForm();
        createForm.setOwnerId(100L);
        createForm.setName("Sample");
        createForm.setItemImg("img.jpg");
        createForm.setDescription("desc");
        createForm.setCategoryId(1L);
        createForm.setPrice(1000);
        createForm.setStatus(ItemStatusEnum.AVAILABLE);
        createForm.setDamagedPolicy("DP");
        createForm.setReturnPolicy("RP");
        createForm.setStartDate(LocalDateTime.now());
        createForm.setEndDate(LocalDateTime.now().plusDays(1));

        sampleItem = Item.builder()
                .itemId(42L)
                .ownerId(createForm.getOwnerId())
                .name(createForm.getName())
                .itemImg(createForm.getItemImg())
                .description(createForm.getDescription())
                .categoryId(createForm.getCategoryId())
                .price(createForm.getPrice())
                .status(createForm.getStatus())
                .damagedPolicy(createForm.getDamagedPolicy())
                .returnPolicy(createForm.getReturnPolicy())
                .startDate(createForm.getStartDate())
                .endDate(createForm.getEndDate())
                .build();

        updateForm = new ItemUpdateForm();
        updateForm.setName("Updated");
        updateForm.setItemImg("new.jpg");
        updateForm.setDescription("new desc");
        updateForm.setCategoryId(2L);
        updateForm.setPrice(2000);
        updateForm.setDamagedPolicy("DP2");
        updateForm.setReturnPolicy("RP2");
        updateForm.setStartDate(createForm.getStartDate().plusDays(1));
        updateForm.setEndDate(createForm.getEndDate().plusDays(2));

        ownerMember = StudentDto.builder().memberId(createForm.getOwnerId()).build();
        otherMember = StudentDto.builder().memberId(999L).build();

        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    }

    @DisplayName("createItem: 새 아이템 생성 후 ID 반환")
    @Test
    void createItem_givenForm_whenSave_thenReturnId() {
        // given
        given(itemRepository.save(any(Item.class)))
                .willAnswer(inv -> {
                    Item arg = inv.getArgument(0);
                    // 반환 시에도 itemId로 세팅
                    return Item.builder()
                            .itemId(sampleItem.getItemId())
                            .ownerId(arg.getOwnerId())
                            .name(arg.getName())
                            .itemImg(arg.getItemImg())
                            .description(arg.getDescription())
                            .price(arg.getPrice())
                            .categoryId(arg.getCategoryId())
                            .status(arg.getStatus())
                            .damagedPolicy(arg.getDamagedPolicy())
                            .returnPolicy(arg.getReturnPolicy())
                            .startDate(arg.getStartDate())
                            .endDate(arg.getEndDate())
                            .build();
                });

        // when
        Long returnedId = itemService.createItem(createForm);

        // then
        assertThat(returnedId).isEqualTo(sampleItem.getItemId());
        then(itemRepository).should().save(any(Item.class));
    }

    @DisplayName("getAllItems: 저장된 모든 아이템을 DTO 목록으로 반환")
    @Test
    void getAllItems_whenCalled_thenReturnDtoList() {
        // given
        Item other = Item.builder()
                .itemId(99L)
                .ownerId(101L)
                .name("Other")
                .itemImg("o.jpg")
                .description("o desc")
                .categoryId(2L)
                .price(1000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("DPo")
                .returnPolicy("RPo")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(3))
                .build();

        Page<Item> itemPage = new PageImpl<>(List.of(sampleItem, other), pageable, 2);
        given(itemRepository.search(any(ItemSearchForm.class), eq(pageable)))
                .willReturn(itemPage);

        // when
        Page<ItemDto> dtoPage = itemService.getAllItems(new ItemSearchForm(), pageable);

        // then
        assertThat(dtoPage).hasSize(2)
                .extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("Sample", "Other");
        then(itemRepository).should().search(any(ItemSearchForm.class), eq(pageable));
    }

    @DisplayName("getAllItems: 키워드 및 가격 범위 조건으로 호출하면, 해당 조건에 맞는 아이템만 반환")
    @Test
    void getAllItems_whenWithConditions_thenReturnFiltered() {
        // given
        ItemSearchForm form = new ItemSearchForm();
        form.setKeyword("Sam");
        form.setMinPrice(500);
        form.setMaxPrice(1500);

        Page<Item> itemPage = new PageImpl<>(List.of(sampleItem), pageable, 1);
        given(itemRepository.search(form, pageable)).willReturn(itemPage);

        // when
        Page<ItemDto> dtoPage = itemService.getAllItems(form, pageable);

        // then
        assertThat(dtoPage).hasSize(1)
                .first()
                .extracting(ItemDto::getName)
                .isEqualTo("Sample");
        then(itemRepository).should().search(form, pageable);
    }

    @DisplayName("getItem: 존재하는 ID면 DTO 반환")
    @Test
    void getItem_existingId_thenReturnDto() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when
        ItemDto dto = itemService.getItem(sampleItem.getItemId());

        // then
        assertThat(dto.getItemId()).isEqualTo(sampleItem.getItemId());
        assertThat(dto.getName()).isEqualTo(sampleItem.getName());
    }

    @DisplayName("getItem: 존재하지 않는 ID면 예외 발생")
    @Test
    void getItem_missingId_thenThrow() {
        // given
        given(itemRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> itemService.getItem(123L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("updateItem: ID 존재 시 필드 업데이트 후 저장")
    @Test
    void updateItem_existing_thenFieldsUpdated() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));
        given(itemRepository.save(any(Item.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        itemService.updateItem(ownerMember, sampleItem.getItemId(), updateForm);

        // then
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        then(itemRepository).should().save(captor.capture());
        Item saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo(updateForm.getName());
        assertThat(saved.getItemImg()).isEqualTo(updateForm.getItemImg());
        assertThat(saved.getCategoryId()).isEqualTo(updateForm.getCategoryId());
        assertThat(saved.getDamagedPolicy()).isEqualTo(updateForm.getDamagedPolicy());
    }

    @DisplayName("updateItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void updateItem_missingId_thenThrowNotFound() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                itemService.updateItem(ownerMember, sampleItem.getItemId(), updateForm)
        )
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("updateItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void updateItem_notOwner_thenThrowUnauthorized() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when, then
        assertThatThrownBy(() ->
                itemService.updateItem(otherMember, sampleItem.getItemId(), updateForm)
        )
                .isInstanceOf(ItemUnauthorizedException.class)
                .hasMessage("자신의 소유 물품이 아닙니다.");
    }

    @DisplayName("deleteItem: ID로 삭제 호출")
    @Test
    void deleteItem_whenCalled_thenRepositoryDeleteById() {
        //given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when
        itemService.deleteItem(ownerMember, sampleItem.getItemId());

        // then
        then(itemRepository).should().deleteById(sampleItem.getItemId());
    }

    @DisplayName("deleteItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void deleteItem_missingId_thenThrowNotFound() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                itemService.deleteItem(ownerMember, sampleItem.getItemId())
        )
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("deleteItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void deleteItem_notOwner_thenThrowUnauthorized() {
        // given
        given(itemRepository.findById(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when, then
        assertThatThrownBy(() ->
                itemService.deleteItem(otherMember, sampleItem.getItemId())
        )
                .isInstanceOf(ItemUnauthorizedException.class)
                .hasMessage("자신의 소유 물품이 아닙니다.");
    }
}