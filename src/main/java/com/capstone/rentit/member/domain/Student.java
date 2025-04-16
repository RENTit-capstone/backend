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
@DiscriminatorValue("STUDENT")
public class Student extends Member {

    @Column
    private String nickname;

    @Column
    private String university;

    @Column
    private String studentId;

    @Column
    private String gender;

    @Column
    private String phone;

    public void updateStudent(String name, String profileImg, String nickname, String phone) {
        super.update(name, profileImg);
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }
}
