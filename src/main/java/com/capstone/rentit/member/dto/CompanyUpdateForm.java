package com.capstone.rentit.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyUpdateForm extends MemberUpdateForm {
    private String companyName;
    private String registrationNumber;
    private String industry;
    private String contactEmail;
    private String description;
}
