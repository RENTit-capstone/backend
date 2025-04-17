package com.capstone.rentit.member.dto;

import com.capstone.rentit.common.MemberRoleEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StudentDto implements MemberDto {
    private Long id;
    private String email;
    private String name;
    private MemberRoleEnum role;
    private String profileImg;
    private LocalDate createdAt;
    private boolean locked;

    private String nickname;
    private String gender;
    private String studentId;
    private String university;
    private String phone;

    @Override
    public boolean getLocked() {
        return locked;
    }
}
