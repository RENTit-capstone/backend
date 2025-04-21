package com.capstone.rentit.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentCreateForm extends MemberCreateForm {
    private String nickname;
    private String university;
    private String studentId;
    private String gender;
    private String phone;
}
