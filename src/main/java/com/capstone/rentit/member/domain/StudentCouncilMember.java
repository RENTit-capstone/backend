package com.capstone.rentit.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DiscriminatorValue("STUDENT_COUNCIL")
public class StudentCouncilMember extends Member {

    @Column
    private String university;

    public void updateCouncilMember(String name, String profileImg) {
        super.update(name, profileImg);
    }
}
