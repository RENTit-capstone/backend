package com.capstone.rentit.member.domain;

import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.MemberRoleEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "member_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRoleEnum role;

    @Column
    private String profileImg;

    @Column
    private LocalDate createdAt;

    @Column
    private boolean locked;

    public abstract void update(MemberUpdateForm form);

    public void updateEntity(String name, String profileImg) {
        if (name != null) {
            this.name = name;
        }
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
    }

    public static Member createEntity(MemberCreateForm form, String encodedPassword) {
        if (form instanceof StudentCreateForm f) {
            return Student.createEntity(f, encodedPassword);
        } else if (form instanceof StudentCouncilMemberCreateForm f) {
            return StudentCouncilMember.createEntity(f, encodedPassword);
        } else if (form instanceof CompanyCreateForm f) {
            return Company.createEntity(f, encodedPassword);
        } else {
            throw new MemberTypeMismatchException("지원하지 않는 회원 유형입니다.");
        }
    }
}
