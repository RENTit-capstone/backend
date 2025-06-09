package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.status.GenderEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentCreateForm extends MemberCreateForm {
    private String nickname;
    private String university;
    private String studentId;
    private GenderEnum gender;
    private String phone;
}
