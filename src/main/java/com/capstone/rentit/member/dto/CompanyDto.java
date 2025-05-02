    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Company;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CompanyDto extends MemberDto {
    private String companyName;

    public static CompanyDto fromEntity(Company entity, String presignedUrl) {
        return CompanyDto.builder()
                .memberId(entity.getMemberId())
                .name(entity.getName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .profileImg(presignedUrl)
                .createdAt(entity.getCreatedAt())
                .locked(entity.isLocked())
                .companyName(entity.getCompanyName())
                .build();
    }
}
