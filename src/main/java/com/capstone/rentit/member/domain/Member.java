package com.capstone.rentit.member.domain;

import com.capstone.rentit.common.MemberRoleEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "member_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Member implements UserDetails {
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

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    public void update(String name, String profileImg) {
        if (name != null) {
            this.name = name;
        }
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
    }

    public void addRole(String role){
        roles.add(role);
    }
}
