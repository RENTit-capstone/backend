package com.capstone.rentit.item.dto;

import com.capstone.rentit.common.ItemStatusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ItemDto {
    private Long itemId;
    private Long ownerId;
    private String name;
    private String itemImg;
    private String description;
    private Long categoryId;
    private ItemStatusEnum status;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
