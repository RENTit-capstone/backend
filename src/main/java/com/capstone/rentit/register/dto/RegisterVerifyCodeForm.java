package com.capstone.rentit.register.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVerifyCodeForm {
    private String email;
    private String university;
    private int code;
}
