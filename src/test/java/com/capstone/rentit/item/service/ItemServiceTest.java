package com.capstone.rentit.item.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.exception.ItemImageMissingException;
import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.exception.ItemUnauthorizedException;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.status.MemberRoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ItemService itemService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private MemberDetailsService memberDetailsService;

    private ItemCreateForm createForm;
    private ItemUpdateForm updateForm;
    private Item sampleItem;
    private MemberDto ownerMember;
    private MemberDto otherMember;
    private Pageable pageable;

    private List<MultipartFile> mockImages;
    private List<String> uploadedKeys;

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 준비

        ownerMember = StudentDto.builder().memberId(100L).role(MemberRoleEnum.STUDENT).build();
        otherMember = StudentDto.builder().memberId(999L).role(MemberRoleEnum.STUDENT).build();

        createForm = ItemCreateForm.builder()
                .name("Sample")
                .description("desc")
                .price(1000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("DP")
                .returnPolicy("RP")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        updateForm = ItemUpdateForm.builder()
                .name("Updated")
                .description("new desc")
                .price(2000)
                .damagedPolicy("DP2")
                .returnPolicy("RP2")
                .startDate(createForm.getStartDate().plusDays(1))
                .endDate(createForm.getEndDate().plusDays(2))
                .build();

        sampleItem = Item.builder()
                .itemId(42L)
                .ownerId(ownerMember.getMemberId())
                .owner(Student.builder()
                        .memberId(ownerMember.getMemberId())
                        .role(ownerMember.getRole())
                        .profileImg("owner/profile.png")
                        .build())
                .name(createForm.getName())
                .description(createForm.getDescription())
                .price(createForm.getPrice())
                .status(createForm.getStatus())
                .damagedPolicy(createForm.getDamagedPolicy())
                .returnPolicy(createForm.getReturnPolicy())
                .startDate(createForm.getStartDate())
                .endDate(createForm.getEndDate())
                .imageKeys(new ArrayList<>(List.of("k1", "k2")))
                .build();

        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        mockImages = List.of(
                new MockMultipartFile("img1", "img1.jpg",
                        "image/jpeg", "dummy".getBytes()),
                new MockMultipartFile("img2", "img2.jpg",
                        "image/jpeg", "dummy".getBytes())
        );
        uploadedKeys = List.of("up1", "up2");

        lenient().when(fileStorageService.store(any(MultipartFile.class)))
                .thenReturn(uploadedKeys.get(0), uploadedKeys.get(1));

        lenient().when(fileStorageService.generatePresignedUrl(anyString()))
                .thenAnswer(inv -> "url://" + inv.getArgument(0));
    }

    @DisplayName("createItem: 새 아이템 생성 후 ID 반환")
    @Test
    void createItem_givenForm_whenSave_thenReturnId() {
        // given
        given(itemRepository.save(any(Item.class)))
                .willAnswer(inv -> {
                    Item arg = inv.getArgument(0);
                    return Item.builder()
                            .itemId(sampleItem.getItemId())
                            .ownerId(arg.getOwnerId())
                            .name(arg.getName())
                            .description(arg.getDescription())
                            .price(arg.getPrice())
                            .status(arg.getStatus())
                            .damagedPolicy(arg.getDamagedPolicy())
                            .returnPolicy(arg.getReturnPolicy())
                            .startDate(arg.getStartDate())
                            .endDate(arg.getEndDate())
                            .imageKeys(new ArrayList<>())
                            .build();
                });

        // when
        Long id = itemService.createItem(otherMember.getMemberId(), createForm, mockImages);

        // then
        assertThat(id).isEqualTo(sampleItem.getItemId());
        then(itemRepository).should(times(1)).save(any(Item.class));
        then(fileStorageService).should(times(2)).store(any(MultipartFile.class));
    }

    @DisplayName("createItem: images가 비어 있으면 ItemImageMissingException")
    @Test
    void createItem_noImages_thenThrowMissingImage() {
        // given
        given(itemRepository.save(any(Item.class)))
                .willAnswer(inv -> {
                    Item arg = inv.getArgument(0);
                    return Item.builder()
                            .itemId(1L)
                            .ownerId(arg.getOwnerId())
                            .name(arg.getName())
                            .build();
                });

        // when & then
        assertThatThrownBy(() ->
                itemService.createItem(
                        ownerMember.getMemberId(),
                        createForm,
                        Collections.emptyList()
                ))
                .isInstanceOf(ItemImageMissingException.class)
                .hasMessage("물품 이미지가 없습니다.");
    }

    @DisplayName("getAllItems: 저장된 모든 아이템을 DTO 목록으로 반환")
    @Test
    void getAllItems_whenCalled_thenReturnDtoList() {
        // given
        Item other = Item.builder()
                .itemId(99L)
                .ownerId(101L)
                .owner(Student.builder()
                        .memberId(otherMember.getMemberId())
                        .role(otherMember.getRole())
                        .build())
                .name("Other")
                .description("o desc")
                .price(1500)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("DPo")
                .returnPolicy("RPo")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .imageKeys(List.of("ko1"))
                .build();

        Page<Item> page = new PageImpl<>(List.of(sampleItem, other), pageable, 2);
        given(itemRepository.search(any(ItemSearchForm.class), eq(pageable)))
                .willReturn(page);

        // when
        Page<ItemSearchResponse> dtoPage =
                itemService.getAllItems(new ItemSearchForm(), pageable);

        // then
        assertThat(dtoPage).hasSize(2)
                .extracting(ItemSearchResponse::getName)
                .containsExactlyInAnyOrder("Sample", "Other");
        then(itemRepository).should()
                .search(any(ItemSearchForm.class), eq(pageable));
    }

    @DisplayName("getAllItems: 키워드 및 가격 범위 조건으로 호출하면, 해당 조건에 맞는 아이템만 반환")
    @Test
    void getAllItems_whenWithConditions_thenReturnFiltered() {
        // given
        ItemSearchForm form = new ItemSearchForm();
        form.setKeyword("Sam");
        form.setMinPrice(500); form.setMaxPrice(1100);

        Page<Item> page = new PageImpl<>(List.of(sampleItem), pageable, 1);
        given(itemRepository.search(form, pageable)).willReturn(page);

        // when
        Page<ItemSearchResponse> dtoPage = itemService.getAllItems(form, pageable);

        // then
        assertThat(dtoPage).hasSize(1)
                .first().extracting(ItemSearchResponse::getName)
                .isEqualTo("Sample");
        then(itemRepository).should().search(form, pageable);
    }

    @DisplayName("getItem: 존재하는 ID면 DTO 반환")
    @Test
    void getItem_existingId_thenReturnDto() {
        // given
        given(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when
        ItemSearchResponse dto = itemService.getItem(sampleItem.getItemId());

        // then
        assertThat(dto.getItemId()).isEqualTo(sampleItem.getItemId());
        assertThat(dto.getImageUrls()).containsExactlyInAnyOrder("url://k1", "url://k2");
    }

    @DisplayName("getItem: 존재하지 않는 ID면 예외 발생")
    @Test
    void getItem_missingId_thenThrow() {
        // given
        given(itemRepository.findWithOwnerByItemId(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(1L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("updateItem: ID 존재 시 필드 업데이트 후 저장")
    @Test
    void updateItem_existing_thenFieldsUpdated() {
        // given
        given(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));
        given(fileStorageService.store(any(MultipartFile.class)))
                .willReturn("new1","new2");


        // when
        itemService.updateItem(ownerMember, sampleItem.getItemId(), updateForm, mockImages);

        // then
        assertThat(sampleItem.getName()).isEqualTo(updateForm.getName());
        assertThat(sampleItem.getImageKeys()).containsExactly("new1","new2");
    }

    @DisplayName("updateItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void updateItem_missingId_thenThrowNotFound() {
        // given
        given(itemRepository.findWithOwnerByItemId(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                itemService.updateItem(ownerMember, 999L, updateForm, mockImages))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @DisplayName("updateItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void updateItem_notOwner_thenThrowUnauthorized() {
        // given
        given(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when, then
        assertThatThrownBy(() ->
                itemService.updateItem(otherMember, sampleItem.getItemId(), updateForm, mockImages))
                .isInstanceOf(ItemUnauthorizedException.class);
    }

    @DisplayName("deleteItem: ID로 삭제 호출")
    @Test
    void deleteItem_whenCalled_thenRepositoryDeleteById() {
        //given
        given(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
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
        given(itemRepository.findWithOwnerByItemId(anyLong())).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() ->
                itemService.deleteItem(ownerMember, 1L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @DisplayName("deleteItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void deleteItem_notOwner_thenThrowUnauthorized() {
        // given
        given(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .willReturn(Optional.of(sampleItem));

        // when, then
        assertThatThrownBy(() ->
                itemService.deleteItem(otherMember, sampleItem.getItemId()))
                .isInstanceOf(ItemUnauthorizedException.class);
    }
}