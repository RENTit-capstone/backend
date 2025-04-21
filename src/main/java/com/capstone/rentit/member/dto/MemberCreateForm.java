package com.capstone.rentit.member.dto;

import com.capstone.rentit.common.MemberRoleEnum;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "memberType" // 클라이언트가 "memberType" 속성으로 타입을 지정 (예: "STUDENT")
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StudentCreateForm.class, name = "STUDENT"),
        @JsonSubTypes.Type(value = StudentCouncilMemberCreateForm.class, name = "COUNCIL"),
        @JsonSubTypes.Type(value = CompanyCreateForm.class, name = "COMPANY")
})
public abstract class MemberCreateForm {
    private String email;
    private String password;
    private String name;
    private String profileImg;
}
