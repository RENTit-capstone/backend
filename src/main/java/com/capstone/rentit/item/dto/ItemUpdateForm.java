package com.capstone.rentit.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ItemUpdateForm {
    private String name;
    private String itemImg;
    private String description;
    private Long categoryId;
    private Integer price;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
