package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.MemberRoleEnum;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@SuperBuilder
public abstract class MemberDto {
    Long memberId;
    String email;
    String name;
    String nickname;
    MemberRoleEnum role;
    String profileImg;
    LocalDate createdAt;
    Boolean locked;

    public static MemberDto fromEntity(Member m, String presignedUrl) {
        if (m instanceof Student s) {
            return StudentDto.fromEntity(s, presignedUrl);
        } else if (m instanceof StudentCouncilMember scm) {
            return StudentCouncilMemberDto.fromEntity(scm, presignedUrl);
        } else if (m instanceof Company c) {
            return CompanyDto.fromEntity(c, presignedUrl);
        } else {
            throw new MemberTypeMismatchException("지원하지 않는 회원 타입입니다.");
        }
    }
}