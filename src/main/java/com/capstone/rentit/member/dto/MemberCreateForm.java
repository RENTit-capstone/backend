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
        @JsonSubTypes.Type(value = StudentCreateForm.class, name = "STUDENT")
        // 추후 다른 타입(예: COMPANY, STUDENT_COUNCIL)이 있으면 여기에 추가
})
public abstract class MemberCreateForm {
    private String email;
    private String password;
    private String name;
    private Integer role;
    private String profileImg;
}
