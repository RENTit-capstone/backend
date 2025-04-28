package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.status.ItemStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ItemCreateForm {
    private Long ownerId;
    private String name;
    private String itemImg;
    private String description;
    private Long categoryId;
    private Integer price;
    private ItemStatusEnum status;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
