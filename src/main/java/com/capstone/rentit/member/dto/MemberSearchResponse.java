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
public abstract class MemberSearchResponse {
    Long memberId;
    String profileImg;

    public static MemberSearchResponse fromEntity(Member m, String presignedUrl) {
        if (m instanceof Student s) {
            return StudentSearchResponse.fromEntity(s, presignedUrl);
        } else if (m instanceof StudentCouncilMember scm) {
            return StudentCouncilMemberSearchResponse.fromEntity(scm, presignedUrl);
        } else if (m instanceof Company c) {
            return CompanySearchResponse.fromEntity(c, presignedUrl);
        } else {
            throw new MemberTypeMismatchException("지원하지 않는 회원 타입입니다.");
        }
    }
}