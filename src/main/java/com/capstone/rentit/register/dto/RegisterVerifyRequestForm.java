package com.capstone.rentit.register.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVerifyRequestForm {
    @NotEmpty private String email;
    @NotEmpty private String university;
}
