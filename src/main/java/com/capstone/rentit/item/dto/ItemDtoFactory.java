package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.domain.Item;

import java.time.LocalDateTime;

public class ItemDtoFactory {
    public static ItemDto toDto(Item item) {
        return ItemDto.builder()
                .itemId(item.getItemId())
                .ownerId(item.getOwnerId())
                .name(item.getName())
                .itemImg(item.getItemImg())
                .description(item.getDescription())
                .categoryId(item.getCategoryId())
                .price(item.getPrice())
                .status(item.getStatus())
                .damagedPolicy(item.getDamagedPolicy())
                .returnPolicy(item.getReturnPolicy())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
