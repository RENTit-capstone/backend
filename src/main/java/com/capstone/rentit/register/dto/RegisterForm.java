package com.capstone.rentit.register.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {
    private String name;
    private String nickname;
    private Integer role;
    private String university;
    private String studentId;
    private String email;
    private String password;
    private Integer gender;
    private String phone;
}
