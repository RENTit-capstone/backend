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
@DiscriminatorValue("STUDENT")
public class Student extends Member {

    @Column
    private String nickname;

    @Column
    private String university;

    @Column
    private String studentId;

    @Column
    private Integer gender;

    @Column
    private String phone;
}
