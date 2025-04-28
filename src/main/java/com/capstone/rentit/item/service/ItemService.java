package com.capstone.rentit.item.service;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Long createItem(ItemCreateForm form) {
        Item item = Item.builder()
                .ownerId(form.getOwnerId())
                .name(form.getName())
                .itemImg(form.getItemImg())
                .description(form.getDescription())
                .categoryId(form.getCategoryId())
                .status(form.getStatus())
                .damagedPolicy(form.getDamagedPolicy())
                .returnPolicy(form.getReturnPolicy())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Item savedItem = itemRepository.save(item);
        return savedItem.getItemId();
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> getAllItems(ItemSearchForm searchForm, Pageable pageable) {
        Page<Item> page = itemRepository.search(searchForm, pageable);
        return page.map(ItemDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public ItemDto getItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return ItemDto.fromEntity(item);
    }

    public void updateItem(Long itemId, ItemUpdateForm form) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.updateItem(form);
        itemRepository.save(item);
    }

    public void deleteItem(Long itemId) {
        itemRepository.deleteById(itemId);
    }
}
