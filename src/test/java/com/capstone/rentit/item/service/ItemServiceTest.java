package com.capstone.rentit.item.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.dto.ItemSearchResponse;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.exception.ItemImageMissingException;
import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.exception.ItemUnauthorizedException;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.item.status.ItemStatusEnum;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ItemService itemService;

    private MemberDto ownerMember;
    private MemberDto otherMember;

    private ItemCreateForm createForm;
    private ItemUpdateForm updateForm;
    private Item sampleItem;

    private Pageable pageable;
    private List<String> initialKeys;
    private List<String> newKeys;

    @BeforeEach
    void setUp() {
        ownerMember = StudentDto.builder()
                .memberId(100L)
                .role(MemberRoleEnum.STUDENT)
                .build();

        otherMember = StudentDto.builder()
                .memberId(999L)
                .role(MemberRoleEnum.STUDENT)
                .build();

        initialKeys = List.of("keyA", "keyB");
        newKeys = List.of("newKey1", "newKey2");

        createForm = ItemCreateForm.builder()
                .name("SampleItem")
                .description("SampleDescription")
                .price(5000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("BasicPolicy")
                .returnPolicy("BasicReturn")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .imageKeys(new ArrayList<>(initialKeys))
                .build();

        updateForm = ItemUpdateForm.builder()
                .name("UpdatedItem")
                .description("UpdatedDescription")
                .price(8000)
                .damagedPolicy("UpdatedDamagedPolicy")
                .returnPolicy("UpdatedReturnPolicy")
                .startDate(createForm.getStartDate().plusDays(2))
                .endDate(createForm.getEndDate().plusDays(2))
                .imageKeys(new ArrayList<>(newKeys))
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
                .imageKeys(new ArrayList<>(initialKeys))
                .build();

        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // presigned URL 생성 부분만 모킹
        lenient().when(fileStorageService.generatePresignedUrl(anyString()))
                .thenAnswer(inv -> "url://" + inv.getArgument(0));
    }

    // ------------ createItem ------------
    @DisplayName("createItem: imageKeys가 있으면 정상 생성 후 ID 반환")
    @Test
    void createItem_withImageKeys_thenReturnId() {
        // *** 중요: 여기서 'when(...).thenAnswer(...)' 구문을 사용 ***
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(inv -> {
                    Item arg = inv.getArgument(0);
                    return Item.builder()
                            .itemId(sampleItem.getItemId())  // 저장된 뒤 ID가 42L인 것처럼
                            .ownerId(arg.getOwnerId())
                            .name(arg.getName())
                            .description(arg.getDescription())
                            .price(arg.getPrice())
                            .status(arg.getStatus())
                            .damagedPolicy(arg.getDamagedPolicy())
                            .returnPolicy(arg.getReturnPolicy())
                            .startDate(arg.getStartDate())
                            .endDate(arg.getEndDate())
                            .imageKeys(arg.getImageKeys())
                            .build();
                });

        Long returnedId = itemService.createItem(ownerMember.getMemberId(), createForm);

        assertThat(returnedId).isEqualTo(42L);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @DisplayName("createItem: imageKeys가 없으면 ItemImageMissingException")
    @Test
    void createItem_withoutImageKeys_thenThrowException() {
        createForm.setImageKeys(Collections.emptyList());

        assertThatThrownBy(() ->
                itemService.createItem(ownerMember.getMemberId(), createForm))
                .isInstanceOf(ItemImageMissingException.class)
                .hasMessage("물품 이미지가 없습니다.");
    }

    // ------------ getAllItems ------------
    @DisplayName("getAllItems: 저장된 모든 아이템을 DTO 목록으로 반환")
    @Test
    void getAllItems_returnDtoList() {
        Page<Item> page = new PageImpl<>(List.of(sampleItem), pageable, 1);
        when(itemRepository.search(any(ItemSearchForm.class), eq(pageable)))
                .thenReturn(page);

        Page<ItemSearchResponse> dtoPage = itemService.getAllItems(new ItemSearchForm(), pageable);

        assertThat(dtoPage).hasSize(1);
        ItemSearchResponse dto = dtoPage.getContent().get(0);
        assertThat(dto.getName()).isEqualTo(sampleItem.getName());
        assertThat(dto.getImageUrls()).containsExactlyInAnyOrder("url://keyA", "url://keyB");
        assertThat(dto.getOwner().getProfileImg()).isEqualTo("url://owner/profile.png");
        verify(itemRepository, times(1)).search(any(ItemSearchForm.class), eq(pageable));
    }

    @DisplayName("getAllItems: 조건에 맞는 아이템만 반환")
    @Test
    void getAllItems_withConditions_thenReturnFiltered() {
        ItemSearchForm form = new ItemSearchForm();
        form.setKeyword("Sample");
        form.setMinPrice(1000);
        form.setMaxPrice(6000);

        Page<Item> page = new PageImpl<>(List.of(sampleItem), pageable, 1);
        when(itemRepository.search(form, pageable))
                .thenReturn(page);

        Page<ItemSearchResponse> dtoPage = itemService.getAllItems(form, pageable);

        assertThat(dtoPage)
                .hasSize(1)
                .first()
                .extracting(ItemSearchResponse::getName)
                .isEqualTo("SampleItem");
        verify(itemRepository).search(form, pageable);
    }

    // ------------ getItem ------------
    @DisplayName("getItem: 존재하는 ID면 DTO 반환")
    @Test
    void getItem_existingId_thenReturnDto() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        ItemSearchResponse dto = itemService.getItem(sampleItem.getItemId());

        assertThat(dto.getItemId()).isEqualTo(sampleItem.getItemId());
        assertThat(dto.getImageUrls()).containsExactlyInAnyOrder("url://keyA", "url://keyB");
        assertThat(dto.getOwner().getProfileImg()).isEqualTo("url://owner/profile.png");
    }

    @DisplayName("getItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void getItem_missingId_thenThrowException() {
        when(itemRepository.findWithOwnerByItemId(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                itemService.getItem(999L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    // ------------ updateItem ------------
    @DisplayName("updateItem: 소유자가 ID 존재시 모든 필드와 imageKeys가 업데이트됨")
    @Test
    void updateItem_existingAndOwner_thenFieldsAndImageKeysUpdated() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        itemService.updateItem(ownerMember, sampleItem.getItemId(), updateForm);

        assertThat(sampleItem.getName()).isEqualTo(updateForm.getName());
        assertThat(sampleItem.getDescription()).isEqualTo(updateForm.getDescription());
        assertThat(sampleItem.getPrice()).isEqualTo(updateForm.getPrice().longValue());
        assertThat(sampleItem.getDamagedPolicy()).isEqualTo(updateForm.getDamagedPolicy());
        assertThat(sampleItem.getReturnPolicy()).isEqualTo(updateForm.getReturnPolicy());
        assertThat(sampleItem.getImageKeys()).containsExactlyElementsOf(newKeys);
    }

    @DisplayName("updateItem: form.imageKeys가 null이면 이미지 키는 변경되지 않고 나머지만 업데이트됨")
    @Test
    void updateItem_withoutImageKeys_thenOnlyOtherFieldsUpdated() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        updateForm.setImageKeys(null);

        itemService.updateItem(ownerMember, sampleItem.getItemId(), updateForm);

        assertThat(sampleItem.getImageKeys()).containsExactlyElementsOf(initialKeys);
        assertThat(sampleItem.getName()).isEqualTo(updateForm.getName());
        assertThat(sampleItem.getDescription()).isEqualTo(updateForm.getDescription());
        assertThat(sampleItem.getPrice()).isEqualTo(updateForm.getPrice().longValue());
    }

    @DisplayName("updateItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void updateItem_missingId_thenThrowNotFound() {
        when(itemRepository.findWithOwnerByItemId(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                itemService.updateItem(ownerMember, 12345L, updateForm))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("updateItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void updateItem_notOwner_thenThrowUnauthorized() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        assertThatThrownBy(() ->
                itemService.updateItem(otherMember, sampleItem.getItemId(), updateForm))
                .isInstanceOf(ItemUnauthorizedException.class)
                .hasMessage("자신의 소유 물품이 아닙니다.");
    }

    // ------------ deleteItem ------------
    @DisplayName("deleteItem: 소유자가 ID 존재 시 정상 삭제 처리")
    @Test
    void deleteItem_existingAndOwner_thenDeleteCalled() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        itemService.deleteItem(ownerMember, sampleItem.getItemId());

        verify(itemRepository, times(1)).deleteById(sampleItem.getItemId());
    }

    @DisplayName("deleteItem: 존재하지 않는 ID면 ItemNotFoundException")
    @Test
    void deleteItem_missingId_thenThrowNotFound() {
        when(itemRepository.findWithOwnerByItemId(anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                itemService.deleteItem(ownerMember, 999L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("존재하지 않는 물품입니다.");
    }

    @DisplayName("deleteItem: 소유자가 아니면 ItemUnauthorizedException")
    @Test
    void deleteItem_notOwner_thenThrowUnauthorized() {
        when(itemRepository.findWithOwnerByItemId(sampleItem.getItemId()))
                .thenReturn(Optional.of(sampleItem));

        assertThatThrownBy(() ->
                itemService.deleteItem(otherMember, sampleItem.getItemId()))
                .isInstanceOf(ItemUnauthorizedException.class)
                .hasMessage("자신의 소유 물품이 아닙니다.");
    }
}