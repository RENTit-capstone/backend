package com.capstone.rentit.item.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ItemUpdateForm {
    private String name;
    private String description;
    private String damagedDescription;
    private Integer price;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> imageKeys;
}
