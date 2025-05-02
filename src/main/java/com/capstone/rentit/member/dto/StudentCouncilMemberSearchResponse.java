    package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.StudentCouncilMember;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

    @Getter
    @SuperBuilder
    public class StudentCouncilMemberSearchResponse extends MemberSearchResponse {
        private String university;

        public static StudentCouncilMemberSearchResponse fromEntity(StudentCouncilMember entity, String presignedUrl) {
            return StudentCouncilMemberSearchResponse.builder()
                    .memberId(entity.getMemberId())
                    .profileImg(presignedUrl)
                    .university(entity.getUniversity())
                    .build();
        }
    }
