package com.capstone.rentit.member.dto;

import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.GenderEnum;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class StudentSearchResponse extends MemberSearchResponse {
    private String nickname;
    private String university;

    public static StudentSearchResponse fromEntity(Student entity, String presignedUrl) {
        return StudentSearchResponse.builder()
                .memberId(entity.getMemberId())
                .profileImg(presignedUrl)
                .nickname(entity.getNickname())
                .university(entity.getUniversity())
                .build();
    }
}
