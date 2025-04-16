package com.capstone.rentit.item.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/items")
    public CommonResponse<Long> createItem(@RequestBody ItemCreateForm form) {
        Long itemId = itemService.createItem(form);
        return new CommonResponse<>(true, itemId, "");
    }

    @GetMapping("/items")
    public CommonResponse<List<ItemDto>> getAllItems() {
        List<ItemDto> items = itemService.getAllItems();
        return new CommonResponse<>(true, items, "");
    }

    @GetMapping("/items/{itemId}")
    public CommonResponse<ItemDto> getItem(@PathVariable("itemId") Long itemId) {
        ItemDto item = itemService.getItem(itemId);
        return new CommonResponse<>(true, item, "");
    }

    @PutMapping("/{itemId}")
    public CommonResponse<ItemDto> updateItem(@PathVariable("itemId") Long itemId,
                                                                   @RequestBody ItemUpdateForm form) {
        itemService.updateItem(itemId, form);
        return new CommonResponse<>(true, null, "");
    }

    @DeleteMapping("/{itemId}")
    public CommonResponse<Void> deleteItem(@PathVariable("itemId") Long itemId) {
        itemService.deleteItem(itemId);
        return new CommonResponse<>(true, null, "");
    }
}
