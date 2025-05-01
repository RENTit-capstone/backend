package com.capstone.rentit.item.domain;

import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
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

    @Column(length = 255)
    private String itemImg;

    @Column(length = 500, nullable = false)
    private String description;

    private Long categoryId;

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

    public void updateItem(ItemUpdateForm form){
        if(form.getName() != null)
            this.name = form.getName();
        if(form.getItemImg() != null)
            this.itemImg = form.getItemImg();
        if(form.getDescription() != null)
            this.description = form.getDescription();
        if(form.getCategoryId() != null)
            this.categoryId = form.getCategoryId();
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
}
