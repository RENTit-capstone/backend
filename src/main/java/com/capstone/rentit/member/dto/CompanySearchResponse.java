    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Company;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

    @Getter
    @SuperBuilder
    public class CompanySearchResponse extends MemberSearchResponse {
        private String companyName;

        public static CompanySearchResponse fromEntity(Company entity, String presignedUrl) {
            return CompanySearchResponse.builder()
                    .memberId(entity.getMemberId())
                    .profileImg(presignedUrl)
                    .companyName(entity.getCompanyName())
                    .build();
        }
    }
