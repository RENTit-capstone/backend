package com.capstone.rentit.member.domain;

import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.dto.StudentUpdateForm;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("STUDENT")
public class Student extends Member {

    @Column
    private String nickname;

    @Column
    private String university;

    @Column
    private String studentId;

    @Column
    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    @Column
    private String phone;

    public static Student createEntity(StudentCreateForm form, String encodedPassword) {
        return Student.builder()
                .name(form.getName())
                .role(MemberRoleEnum.STUDENT)
                .email(form.getEmail())
                .password(encodedPassword)
                .locked(false)
                .createdAt(LocalDate.now())
                .gender(form.getGender())
                .studentId(form.getStudentId())
                .university(form.getUniversity())
                .nickname(form.getNickname())
                .phone(form.getPhone())
                .build();
    }

    @Override
    public void update(MemberUpdateForm form) {
        if (!(form instanceof StudentUpdateForm f)) {
            throw new MemberTypeMismatchException("학생 정보 수정 폼이 아닙니다.");
        }
        updateEntity(form.getName());
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }
}
