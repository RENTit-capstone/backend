package com.capstone.rentit.member.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "memberType" //클라이언트에서 전달받음
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StudentUpdateForm.class, name = "STUDENT"),
        @JsonSubTypes.Type(value = StudentCouncilMemberUpdateForm.class, name = "COUNCIL"),
        @JsonSubTypes.Type(value = CompanyUpdateForm.class, name = "COMPANY")
})
public abstract class MemberUpdateForm {
    private String name;
    private String profileImg;
}
