    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.StudentCouncilMember;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StudentCouncilMemberDto extends MemberDto {
    private String university;

    public static StudentCouncilMemberDto fromEntity(StudentCouncilMember entity) {
        return StudentCouncilMemberDto.builder()
                .memberId(entity.getMemberId())
                .name(entity.getName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .profileImg(entity.getProfileImg())
                .createdAt(entity.getCreatedAt())
                .locked(entity.isLocked())
                .university(entity.getUniversity())
                .build();
    }
}
