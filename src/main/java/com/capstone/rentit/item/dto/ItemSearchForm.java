package com.capstone.rentit.item.dto;

import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ItemSearchForm {
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private Integer minPrice;
    private Integer maxPrice;

    private ItemStatusEnum status;
    private List<MemberRoleEnum> ownerRoles;

    private String university;
}
