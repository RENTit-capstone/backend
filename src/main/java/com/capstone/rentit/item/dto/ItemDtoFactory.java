package com.capstone.rentit.item.dto;

import com.capstone.rentit.common.ItemStatusConverter;
import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.CompanyDto;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentCouncilMemberDto;
import com.capstone.rentit.member.dto.StudentDto;

import java.time.LocalDateTime;

public class ItemDtoFactory {
    public static ItemDto toDto(Item item) {
        return ItemDto.builder()
                .itemId(item.getItemId())
                .ownerId(item.getOwnerId())
                .name(item.getName())
                .itemImg(item.getItemImg())
                .description(item.getDescription())
                .categoryId(item.getCategoryId())
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
