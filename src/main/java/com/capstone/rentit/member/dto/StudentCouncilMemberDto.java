    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.status.MemberRoleEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StudentCouncilMemberDto implements MemberDto {
    private Long id;
    private String email;
    private String name;
    private MemberRoleEnum role;
    private String profileImg;
    private LocalDate createdAt;
    private boolean locked;

    private String university;

    @Override
    public boolean getLocked() {
        return locked;
    }
}
