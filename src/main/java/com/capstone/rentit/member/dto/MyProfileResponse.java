package com.capstone.rentit.member.dto;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.dto.ItemBriefResponse;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalBriefResponse;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.capstone.rentit.member.domain.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MyProfileResponse {
    private Long   memberId;
    private String email;
    private String name;
    private String nickname;
    private String role;
    private String profileImg;
    private String university;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;

    /* 소유 물품 ------------------------------- */
    private List<ItemBriefResponse> items;

    /* 대여(소유자) ---------------------------- */
    private List<RentalBriefResponse> ownedRentals;

    /* 대여(대여자) ---------------------------- */
    private List<RentalBriefResponse> rentedRentals;

    public static MyProfileResponse fromEntity(Member m,
                                    List<ItemBriefResponse> items,
                                    List<RentalBriefResponse> owned,
                                    List<RentalBriefResponse> rented,
                                               String presignedUrl) {
        if(m.getRole() == MemberRoleEnum.STUDENT){
            return MyProfileResponse.builder()
                    .memberId(m.getMemberId())
                    .email(m.getEmail())
                    .name(m.getName())
                    .nickname(m.getNickname())
                    .role(m.getRole().name())
                    .profileImg(presignedUrl)
                    .createdAt(m.getCreatedAt())
                    .items(items)
                    .ownedRentals(owned)
                    .rentedRentals(rented)
                    .university(((Student)m).getUniversity())
                    .build();
        }
        return MyProfileResponse.builder()
                .memberId(m.getMemberId())
                .email(m.getEmail())
                .name(m.getName())
                .nickname(m.getNickname())
                .role(m.getRole().name())
                .profileImg(presignedUrl)
                .createdAt(m.getCreatedAt())
                .items(items)
                .ownedRentals(owned)
                .rentedRentals(rented)
                .build();
    }
}
