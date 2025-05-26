package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Admin;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.GenderEnum;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class AdminDto extends MemberDto {

    public static AdminDto fromEntity(Admin entity) {
        return AdminDto.builder()
                .memberId(entity.getMemberId())
                .name(entity.getName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .locked(entity.isLocked())
                .nickname(entity.getNickname())
                .build();
    }
}
