package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemBriefResponse {

    private Long itemId;
    private String name;
    private long price;
    private ItemStatusEnum status;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    public static ItemBriefResponse fromEntity(Item item, String presignedUrl) {
        return ItemBriefResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .price(item.getPrice())
                .status(item.getStatus())
                .thumbnailUrl(presignedUrl)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
