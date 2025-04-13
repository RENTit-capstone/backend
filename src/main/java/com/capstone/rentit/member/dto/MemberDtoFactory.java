package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;

public class MemberDtoFactory {
    public static MemberDto toDto(Member member) {
        return switch (member) {
            case Student student -> StudentDto.builder()
                    .id(student.getId())
                    .email(student.getEmail())
                    .role(student.getRole())
                    .locked(student.isLocked())
                    .profileImg(student.getProfileImg())
                    .createdAt(student.getCreatedAt())
                    .name(student.getName())
                    .nickname(student.getNickname())
                    .gender(student.getGender())
                    .studentId(student.getStudentId())
                    .university(student.getUniversity())
                    .phone(student.getPhone())
                    .build();
            case StudentCouncilMember scm -> StudentCouncilMemberDto.builder()
                    .id(scm.getId())
                    .email(scm.getEmail())
                    .role(scm.getRole())
                    .locked(scm.isLocked())
                    .profileImg(scm.getProfileImg())
                    .createdAt(scm.getCreatedAt())
                    .university(scm.getUniversity())
                    .build();
            case Company company -> CompanyDto.builder()
                    .id(company.getId())
                    .email(company.getEmail())
                    .role(company.getRole())
                    .locked(company.isLocked())
                    .profileImg(company.getProfileImg())
                    .createdAt(company.getCreatedAt())
                    .companyName(company.getCompanyName())
                    .build();
            case null, default -> throw new IllegalArgumentException("Unknown member type: " + member.getClass());
        };
    }
}
