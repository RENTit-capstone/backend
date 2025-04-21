package com.capstone.rentit.item.service;

import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemUpdateForm;

import java.util.List;

public interface ItemService {
    Long createItem(ItemCreateForm form);
    List<ItemDto> getAllItems();
    ItemDto getItem(Long itemId);
    void updateItem(Long itemId, ItemUpdateForm form);
    void deleteItem(Long itemId);
}
