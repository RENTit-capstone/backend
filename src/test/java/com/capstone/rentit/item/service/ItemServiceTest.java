package com.capstone.rentit.item.service;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // 각 테스트 후 롤백되도록 설정
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * createItem() 테스트
     * - ItemCreateForm을 전달해 항목을 생성한 후, 아이디 반환 및 DB에 저장되었는지 검증
     */
    @Test
    void testCreateItem() {
        // given : 테스트를 위한 ItemCreateForm 생성
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(100L);
        form.setName("Test Item");
        form.setItemImg("http://example.com/image.jpg");
        form.setDescription("Test description");
        form.setCategoryId(1L);
        form.setStatus(0);  // status가 정수값으로 들어오면, 내부에서 ItemStatusConverter.fromInteger() 호출
        form.setDamagedPolicy("Damage policy");
        form.setReturnPolicy("Return policy");
        form.setStartDate(LocalDateTime.of(2025, 1, 1, 9, 0));
        form.setEndDate(LocalDateTime.of(2025, 12, 31, 18, 0));

        // when : 항목 생성
        Long savedId = itemService.createItem(form);

        // then : 반환된 ID가 null이 아니며, DB에 실제 저장되었는지 검증
        assertNotNull(savedId);
        Item found = itemRepository.findById(savedId).orElse(null);
        assertNotNull(found);
        assertEquals("Test Item", found.getName());
    }

    /**
     * getAllItems() 테스트
     * - 두 개 이상의 항목을 생성한 후, 전체 목록이 DTO로 정상적으로 반환되는지 확인
     */
    @Test
    void testGetAllItems() {
        // given : 항목 2개 생성
        ItemCreateForm form1 = new ItemCreateForm();
        form1.setOwnerId(100L);
        form1.setName("Item 1");
        form1.setItemImg("http://example.com/1.jpg");
        form1.setDescription("Description 1");
        form1.setCategoryId(1L);
        form1.setStatus(0);
        form1.setDamagedPolicy("Policy 1");
        form1.setReturnPolicy("Return 1");
        form1.setStartDate(LocalDateTime.now());
        form1.setEndDate(LocalDateTime.now().plusDays(10));

        ItemCreateForm form2 = new ItemCreateForm();
        form2.setOwnerId(101L);
        form2.setName("Item 2");
        form2.setItemImg("http://example.com/2.jpg");
        form2.setDescription("Description 2");
        form2.setCategoryId(2L);
        form2.setStatus(1);
        form2.setDamagedPolicy("Policy 2");
        form2.setReturnPolicy("Return 2");
        form2.setStartDate(LocalDateTime.now());
        form2.setEndDate(LocalDateTime.now().plusDays(5));

        itemService.createItem(form1);
        itemService.createItem(form2);

        // when : 전체 항목 조회
        List<ItemDto> allItems = itemService.getAllItems();

        // then : 반환되는 목록이 null이 아니고, 최소 2개 이상의 항목이 포함됨
        assertNotNull(allItems);
        assertTrue(allItems.size() >= 2);
    }

    /**
     * getItem() 테스트
     * - 존재하는 itemId에 대해 DTO를 반환하는지, 존재하지 않는 경우 예외가 발생하는지 검증
     */
    @Test
    void testGetItem() {
        // given : 항목 생성
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(100L);
        form.setName("Get Item Test");
        form.setItemImg("http://example.com/item.jpg");
        form.setDescription("Description");
        form.setCategoryId(1L);
        form.setStatus(0);
        form.setDamagedPolicy("Policy");
        form.setReturnPolicy("Return");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(3));
        Long savedId = itemService.createItem(form);

        // when : 존재하는 항목 조회
        ItemDto dto = itemService.getItem(savedId);

        // then : DTO가 null이 아니고, 이름이 일치하는지 검증
        assertNotNull(dto);
        assertEquals("Get Item Test", dto.getName());

        // 없는 itemId 테스트 : RuntimeException 발생
        Exception exception = assertThrows(RuntimeException.class, () -> itemService.getItem(999999L));
        assertEquals("Item not found", exception.getMessage());
    }

    /**
     * updateItem() 테스트
     * - 기존 항목을 업데이트한 후, 변경 내용이 DB에 반영되었는지 검증
     */
    @Test
    void testUpdateItem() {
        // given : 항목 생성
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(100L);
        form.setName("Original Name");
        form.setItemImg("http://example.com/original.jpg");
        form.setDescription("Original description");
        form.setCategoryId(1L);
        form.setStatus(0);
        form.setDamagedPolicy("Original policy");
        form.setReturnPolicy("Original return");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(2));
        Long savedId = itemService.createItem(form);

        // when : 업데이트 폼 생성 및 업데이트 수행 (몇몇 필드 변경)
        ItemUpdateForm updateForm = new ItemUpdateForm();
        updateForm.setName("Updated Name");
        updateForm.setItemImg("http://example.com/updated.jpg");
        updateForm.setDescription("Updated description");
        updateForm.setCategoryId(2L);
        updateForm.setStatus(1);
        updateForm.setDamagedPolicy("Updated policy");
        updateForm.setReturnPolicy("Updated return");
        updateForm.setStartDate(LocalDateTime.now().plusDays(1));
        updateForm.setEndDate(LocalDateTime.now().plusDays(5));
        itemService.updateItem(savedId, updateForm);

        // then : 업데이트된 항목 조회 및 필드 값 검증
        ItemDto updatedDto = itemService.getItem(savedId);
        assertNotNull(updatedDto);
        assertEquals("Updated Name", updatedDto.getName());
        assertEquals("http://example.com/updated.jpg", updatedDto.getItemImg());
        assertEquals("Updated description", updatedDto.getDescription());
        assertEquals(2L, updatedDto.getCategoryId());
        assertEquals("Updated policy", updatedDto.getDamagedPolicy());
        assertEquals("Updated return", updatedDto.getReturnPolicy());
    }

    /**
     * deleteItem() 테스트
     * - 항목 삭제 후, DB에서 실제 삭제되었는지 검증
     */
    @Test
    void testDeleteItem() {
        // given : 항목 생성
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(100L);
        form.setName("Item to Delete");
        form.setItemImg("http://example.com/delete.jpg");
        form.setDescription("Description");
        form.setCategoryId(1L);
        form.setStatus(0);
        form.setDamagedPolicy("Policy");
        form.setReturnPolicy("Return");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(3));
        Long savedId = itemService.createItem(form);
        assertNotNull(itemRepository.findById(savedId).orElse(null));

        // when : 항목 삭제
        itemService.deleteItem(savedId);

        // then : 해당 항목이 DB에서 삭제되었는지 확인
        Optional<Item> deleted = itemRepository.findById(savedId);
        assertFalse(deleted.isPresent());
    }
}