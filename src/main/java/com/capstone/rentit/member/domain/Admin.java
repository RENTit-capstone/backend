package com.capstone.rentit.member.domain;

import com.capstone.rentit.member.dto.*;
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
@DiscriminatorValue("ADMIN")
public class Admin extends Member {

    public static Admin createEntity(AdminCreateForm form, String encodedPassword) {
        return Admin.builder()
                .name(form.getName())
                .nickname(form.getName())
                .role(MemberRoleEnum.ADMIN)
                .email(form.getEmail())
                .password(encodedPassword)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
    }

    @Override
    public void update(MemberUpdateForm form) {
        return;
    }
}
