package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ItemDto {
    private Long itemId;
    private Long ownerId;
    private String name;
    private List<String> imageKeys;
    private String description;
    private String damagedDescription;
    private long price;
    private ItemStatusEnum status;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ItemDto fromEntity(Item item) {
        return ItemDto.builder()
                .itemId(item.getItemId())
                .ownerId(item.getOwnerId())
                .name(item.getName())
                .imageKeys(item.getImageKeys())
                .description(item.getDescription())
                .damagedDescription(item.getDamagedDescription())
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
