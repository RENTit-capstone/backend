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

    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatusEnum status;

    @Column(length = 1000, nullable = false)
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
                .status(form.getStatus())
                .damagedPolicy(form.getDamagedPolicy())
                .returnPolicy(form.getReturnPolicy())
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
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
        updatedAt = LocalDateTime.now();
    }

    public void updateAvailable(){
        status = ItemStatusEnum.AVAILABLE;
    }

    public void updateOut(){
        status = ItemStatusEnum.OUT;
    }

    public void addImageKey(String key) {
        this.imageKeys.add(key);
    }

    public void clearImageKeys() {           // (업데이트 시 활용)
        this.imageKeys.clear();
    }
}
