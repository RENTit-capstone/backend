package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.status.ItemStatusEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ItemCreateForm {
    @NotEmpty private String name;
    @NotEmpty private String description;
    private String damagedDescription;
    @NotNull private Integer price;
    @NotNull private ItemStatusEnum status;
    @NotEmpty private String damagedPolicy;
    private String returnPolicy;
    @NotNull private LocalDateTime startDate;
    @NotNull private LocalDateTime endDate;
    private List<String> imageKeys;
}
