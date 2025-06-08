package com.capstone.rentit.item.domain;

import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "item",
        indexes = {
                @Index(name = "idx_item_owner", columnList = "owner_id"),
                @Index(name = "idx_item_status", columnList = "status"),
                @Index(name = "idx_item_status_price", columnList = "status, price"),
                @Index(name = "idx_item_start_date", columnList = "start_date"),
                @Index(name = "idx_item_end_date", columnList = "end_date")
        }
)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", insertable = false, updatable = false)
    private Member owner;

    @Column(length = 50, nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "item_image_keys",
            joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "object_key", nullable = false, length = 255)
    @Builder.Default
    private List<String> imageKeys = new ArrayList<>();

    @Column(length = 500, nullable = false)
    private String description;

    @Column(length = 500)
    private String damagedDescription;

    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatusEnum status;

    @Column(length = 5000, nullable = false)
    private String damagedPolicy;

    @Column(length = 1000, nullable = false)
    private String returnPolicy;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Item createItem(Long ownerId, ItemCreateForm form){
        return Item.builder()
                .ownerId(ownerId)
                .name(form.getName())
                .description(form.getDescription())
                .damagedDescription(form.getDamagedDescription())
                .price(form.getPrice())
                .status(form.getStatus())
                .damagedPolicy(form.getDamagedPolicy())
                .returnPolicy(form.getReturnPolicy())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .imageKeys(form.getImageKeys())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateItem(ItemUpdateForm form){
        if(form == null) return;
        if(form.getName() != null)
            this.name = form.getName();
        if(form.getDescription() != null)
            this.description = form.getDescription();
        if(form.getPrice() != null)
            this.price = form.getPrice();
        if(form.getDamagedPolicy() != null)
            this.damagedPolicy = form.getDamagedPolicy();
        if(form.getReturnPolicy() != null)
            this.returnPolicy = form.getReturnPolicy();
        if(form.getDescription() != null)
            this.description = form.getDescription();
        if(form.getDescription() != null)
            this.description = form.getDescription();
        if(form.getImageKeys() != null && !form.getImageKeys().isEmpty())
            this.imageKeys = form.getImageKeys();
        updatedAt = LocalDateTime.now();
    }

    public void updateAvailable(){
        status = ItemStatusEnum.AVAILABLE;
    }

    public void updateOut(){
        status = ItemStatusEnum.OUT;
    }

    public void deleteItem(){
        status = ItemStatusEnum.DELETED;
    }
}
