package com.capstone.rentit.member.domain;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.exception.MemberTypeMismatchException;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.rental.domain.Rental;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRoleEnum role;

    @Column
    private String profileImg;

    @Column
    private LocalDate createdAt;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("createdAt DESC")
    @OrderColumn(name = "item_idx")
    private Set<Item> items = new LinkedHashSet<>();

    @OneToMany(mappedBy="ownerMember", fetch = FetchType.LAZY)
    @Builder.Default
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("requestDate DESC")
    private Set<Rental> ownedRentals = new LinkedHashSet<>();

    @OneToMany(mappedBy="renterMember", fetch = FetchType.LAZY)
    @Builder.Default
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("requestDate DESC")
    private Set<Rental> rentedRentals = new LinkedHashSet<>();

    @Column
    private boolean locked;

    @Column(length = 255)
    private String fcmToken;

    public abstract void update(MemberUpdateForm form);

    public void updateEntity(String name, String nickname, String imageKey) {
        if (name != null)
            this.name = nickname;
        if (nickname != null)
            this.name = nickname;
        if (imageKey != null) 
            this.profileImg = imageKey;
    }

    public static Member createEntity(MemberCreateForm form, String encodedPassword) {
        if (form instanceof StudentCreateForm f) {
            return Student.createEntity(f, encodedPassword);
        } else if (form instanceof StudentCouncilMemberCreateForm f) {
            return StudentCouncilMember.createEntity(f, encodedPassword);
        } else if (form instanceof CompanyCreateForm f) {
            return Company.createEntity(f, encodedPassword);
        } else if (form instanceof AdminCreateForm f) {
            return Admin.createEntity(f, encodedPassword);
        } else {
            throw new MemberTypeMismatchException("지원하지 않는 회원 유형입니다.");
        }
    }

    public void updateFcmToken(String token) {
        this.fcmToken = token;
    }
}
