package com.capstone.rentit.item.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.service.ItemService;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/items")
    public CommonResponse<Long> createItem(@RequestBody ItemCreateForm form) {
        Long itemId = itemService.createItem(form);
        return CommonResponse.success(itemId);
    }

    @GetMapping("/items")
    public CommonResponse<List<ItemDto>> getAllItems() {
        List<ItemDto> items = itemService.getAllItems();
        return CommonResponse.success(items);
    }

    @GetMapping("/items/{itemId}")
    public CommonResponse<ItemDto> getItem(@PathVariable("itemId") Long itemId) {
        ItemDto item = itemService.getItem(itemId);
        return CommonResponse.success(item);
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/items/{itemId}")
    public CommonResponse<Void> updateItem(
            @PathVariable("itemId") Long itemId,
            @RequestBody ItemUpdateForm form,
            @Login MemberDto loginMember) throws AccessDeniedException {

        ItemDto existing = itemService.getItem(itemId);
        if (!existing.getOwnerId().equals(loginMember.getId())) {
            return CommonResponse.failure("소유자 매칭 오류");
        }

        itemService.updateItem(itemId, form);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/items/{itemId}")
    public CommonResponse<Void> deleteItem(
            @PathVariable("itemId") Long itemId,
            @Login MemberDto loginMember) throws AccessDeniedException {

        // 소유자 확인
        ItemDto existing = itemService.getItem(itemId);
        if (!existing.getOwnerId().equals(loginMember.getId())) {
            return CommonResponse.failure("소유자 매칭 오류");
        }

        itemService.deleteItem(itemId);
        return CommonResponse.success(null);
    }
}
