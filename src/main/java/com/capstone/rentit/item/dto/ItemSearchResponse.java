package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.MemberSearchResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ItemSearchResponse {
    private Long itemId;
    private MemberSearchResponse owner;
    private String name;
    private List<String> imageKeys;
    private List<String> imageUrls;
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

    public static ItemSearchResponse fromEntity(Item item, List<String> imageUrls, String ownerProfileImg) {
        return ItemSearchResponse.builder()
                .itemId(item.getItemId())
                .owner(MemberSearchResponse.fromEntity(item.getOwner(), ownerProfileImg))
                .name(item.getName())
                .imageKeys(item.getImageKeys())
                .imageUrls(imageUrls)
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
