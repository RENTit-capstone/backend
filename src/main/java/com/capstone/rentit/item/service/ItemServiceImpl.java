package com.capstone.rentit.item.service;

import com.capstone.rentit.common.ItemStatusConverter;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemDtoFactory;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    public Long createItem(ItemCreateForm form) {
        Item item = Item.builder()
                .ownerId(form.getOwnerId())
                .name(form.getName())
                .itemImg(form.getItemImg())
                .description(form.getDescription())
                .categoryId(form.getCategoryId())
                .status(ItemStatusConverter.fromInteger(form.getStatus()))
                .damagedPolicy(form.getDamagedPolicy())
                .returnPolicy(form.getReturnPolicy())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        itemRepository.save(item);
        return item.getItemId();
    }

    @Override
    public List<ItemDto> getAllItems() {
        List<Item> items = itemRepository.findAll();
        return items.stream().map(ItemDtoFactory::toDto).collect(Collectors.toList());
    }

    @Override
    public ItemDto getItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return ItemDtoFactory.toDto(item);
    }

    @Override
    public void updateItem(Long itemId, ItemUpdateForm form) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.updateItem(form);
        itemRepository.save(item);
    }

    @Override
    public void deleteItem(Long itemId) {
        itemRepository.deleteById(itemId);
    }
}
