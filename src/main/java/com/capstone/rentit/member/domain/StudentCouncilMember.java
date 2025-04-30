package com.capstone.rentit.member.domain;

import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.dto.StudentCouncilMemberCreateForm;
import com.capstone.rentit.member.dto.StudentCouncilMemberUpdateForm;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.MemberRoleEnum;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("STUDENT_COUNCIL")
public class StudentCouncilMember extends Member {

    @Column
    private String university;

    @Override
    public void update(MemberUpdateForm form) {
        if (!(form instanceof StudentCouncilMemberUpdateForm f)) {
            throw new MemberTypeMismatchException("학생회 정보 수정 폼이 아닙니다.");
        }
        super.updateEntity(f.getName());
    }

    public static StudentCouncilMember createEntity(StudentCouncilMemberCreateForm form, String encodedPassword) {
        return StudentCouncilMember.builder()
                .name(form.getName())
                .role(MemberRoleEnum.COUNCIL)
                .email(form.getEmail())
                .password(encodedPassword)
                .locked(false)
                .createdAt(LocalDate.now())
                .university(form.getUniversity())
                .build();
    }
}
