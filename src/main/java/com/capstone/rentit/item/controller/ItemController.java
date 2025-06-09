package com.capstone.rentit.item.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.service.ItemService;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/items", consumes = "application/json")
    public CommonResponse<Long> createItem(@Login MemberDto loginMember,
                                           @RequestBody @Valid ItemCreateForm form) {

        Long itemId = itemService.createItem(loginMember.getMemberId(), form);
        return CommonResponse.success(itemId);
    }

    @GetMapping("/items")
    public CommonResponse<Page<ItemSearchResponse>> getAllItems(
            @ModelAttribute("form") ItemSearchForm searchForm,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ItemSearchResponse> page = itemService.getAllItems(searchForm, pageable);
        return CommonResponse.success(page);
    }

    @GetMapping("/items/{itemId}")
    public CommonResponse<ItemSearchResponse> getItem(@PathVariable("itemId") Long itemId) {

        ItemSearchResponse item = itemService.getItem(itemId);
        return CommonResponse.success(item);
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping(value = "/items/{itemId}", consumes = "application/json")
    public CommonResponse<Void> updateItem(
            @PathVariable("itemId") Long itemId,
            @RequestBody ItemUpdateForm form,
            @Login MemberDto loginMember) {

        itemService.updateItem(loginMember, itemId, form);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/items/{itemId}")
    public CommonResponse<Void> deleteItem(
            @PathVariable("itemId") Long itemId,
            @Login MemberDto loginMember) {
        itemService.deleteItem(loginMember, itemId);
        return CommonResponse.success(null);
    }
}
