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
        if (m.getRole() == MemberRoleEnum.STUDENT) {
            return StudentSearchResponse.fromEntity((Student) m, presignedUrl);
        } else if (m.getRole() == MemberRoleEnum.COUNCIL) {
            return StudentCouncilMemberSearchResponse.fromEntity((StudentCouncilMember) m, presignedUrl);
        } else if (m.getRole() == MemberRoleEnum.COMPANY) {
            return CompanySearchResponse.fromEntity((Company) m, presignedUrl);
        } else {
            throw new MemberTypeMismatchException("지원하지 않는 회원 타입입니다. [" + m.getRole() + "]");
        }
    }
}