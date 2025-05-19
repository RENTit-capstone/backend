package com.capstone.rentit.member.domain;

import com.capstone.rentit.member.dto.CompanyCreateForm;
import com.capstone.rentit.member.dto.CompanyUpdateForm;
import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.MemberRoleEnum;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("COMPANY")
public class Company extends Member {

    @Column
    private String companyName;

    @Column
    private String registrationNumber;

    @Column
    private String industry;

    @Column
    private String contactEmail;

    @Column
    private String description;

    @Override
    public void update(MemberUpdateForm form) {
        if(form == null) return;
        if (!(form instanceof CompanyUpdateForm f)) {
            throw new MemberTypeMismatchException("회사 정보 수정 폼이 아닙니다.");
        }
        super.updateEntity(form.getName());
        if(f.getCompanyName() != null)
            this.companyName = companyName;
        if(f.getRegistrationNumber() != null)
            this.registrationNumber = registrationNumber;
        if(f.getIndustry() != null)
            this.industry = industry;
        if(f.getDescription() != null)
            this.description = description;
    }

    public static Company createEntity(CompanyCreateForm form, String encodedPassword) {
        return Company.builder()
                .name(form.getName())
                .role(MemberRoleEnum.COMPANY)
                .email(form.getEmail())
                .password(encodedPassword)
                .locked(false)
                .createdAt(LocalDate.now())
                .companyName(form.getCompanyName())
                .registrationNumber(form.getRegistrationNumber())
                .industry(form.getIndustry())
                .description(form.getDescription())
                .build();
    }
}
