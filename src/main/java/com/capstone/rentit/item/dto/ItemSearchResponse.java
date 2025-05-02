package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.dto.MemberDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ItemSearchResponse {
    private Long itemId;
    private MemberDto owner;
    private String name;
    private String itemImg;
    private String description;
    private Integer price;
    private ItemStatusEnum status;
    private String damagedPolicy;
    private String returnPolicy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ItemSearchResponse fromEntity(Item item, String presignedUrl) {
        return ItemSearchResponse.builder()
                .itemId(item.getItemId())
                .owner(MemberDto.fromEntity(item.getOwner(), presignedUrl))
                .name(item.getName())
                .itemImg(item.getItemImg())
                .description(item.getDescription())
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
