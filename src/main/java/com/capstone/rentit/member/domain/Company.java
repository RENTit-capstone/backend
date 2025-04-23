package com.capstone.rentit.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("COMPANY")
public class Company extends Member {

    @Column
    private String companyName;

    public void updateCompany(String name, String profileImg, String companyName) {
        super.update(name, profileImg);
        if (companyName != null) {
            this.companyName = companyName;
        }
    }
}
