package com.capstone.rentit.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentUpdateForm extends MemberUpdateForm {
    private String nickname;
    private String phone;
}
