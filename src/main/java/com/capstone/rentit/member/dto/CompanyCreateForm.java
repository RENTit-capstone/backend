package com.capstone.rentit.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyCreateForm extends MemberCreateForm {
    private String companyName;
}
