package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.GenderEnum;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StudentDto extends MemberDto {
    private String nickname;
    private GenderEnum gender;
    private String studentId;
    private String university;
    private String phone;

    public static StudentDto fromEntity(Student entity) {
        return StudentDto.builder()
                .memberId(entity.getMemberId())
                .name(entity.getName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .profileImg(entity.getProfileImg())
                .createdAt(entity.getCreatedAt())
                .locked(entity.isLocked())
                .nickname(entity.getNickname())
                .gender(entity.getGender())
                .studentId(entity.getStudentId())
                .university(entity.getUniversity())
                .phone(entity.getPhone())
                .build();
    }
}
