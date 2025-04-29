package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;

public class MemberDtoFactory {
    public static MemberDto toDto(Member member) {
        if (member.getRole().equals(MemberRoleEnum.STUDENT)) {
            Student student = (Student) member;
            return StudentDto.builder()
                    .id(student.getMemberId())
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
        } else if (member.getRole().equals(MemberRoleEnum.COUNCIL)) {
            StudentCouncilMember scm = (StudentCouncilMember) member;
            return StudentCouncilMemberDto.builder()
                    .id(scm.getMemberId())
                    .email(scm.getEmail())
                    .role(scm.getRole())
                    .locked(scm.isLocked())
                    .profileImg(scm.getProfileImg())
                    .createdAt(scm.getCreatedAt())
                    .university(scm.getUniversity())
                    .build();
        } else if (member.getRole().equals(MemberRoleEnum.COMPANY)) {
            Company company = (Company) member;
            return CompanyDto.builder()
                    .id(company.getMemberId())
                    .email(company.getEmail())
                    .role(company.getRole())
                    .locked(company.isLocked())
                    .profileImg(company.getProfileImg())
                    .createdAt(company.getCreatedAt())
                    .companyName(company.getCompanyName())
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown member type: " + member.getClass());
        }
    }
}
