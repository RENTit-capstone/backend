package com.capstone.rentit.member.dto;

import com.capstone.rentit.common.MemberRoleEnum;

import java.time.LocalDate;

public interface MemberDto {
    Long getId();
    String getEmail();
    String getName();
    MemberRoleEnum getRole();
    String getProfileImg();
    LocalDate getCreatedAt();
    boolean getLocked();
}