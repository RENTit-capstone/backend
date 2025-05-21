    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.StudentCouncilMember;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StudentCouncilMemberDto extends MemberDto {
    private String university;

    public static StudentCouncilMemberDto fromEntity(StudentCouncilMember entity, String presignedUrl) {
        return StudentCouncilMemberDto.builder()
                .memberId(entity.getMemberId())
                .name(entity.getName())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .role(entity.getRole())
                .profileImg(presignedUrl)
                .createdAt(entity.getCreatedAt())
                .locked(entity.isLocked())
                .university(entity.getUniversity())
                .build();
    }
}
