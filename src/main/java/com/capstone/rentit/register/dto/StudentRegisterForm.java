package com.capstone.rentit.register.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRegisterForm {
    private String email;
    private String password;
    private String name;
    private Integer role;

    private String nickname;
    private String university;
    private String studentId;
    private Integer gender;
    private String phone;
}
