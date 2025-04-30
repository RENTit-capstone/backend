package com.capstone.rentit.member.status;

public enum MemberRoleEnum {
    STUDENT, COUNCIL, COMPANY, ADMIN;

    public static String roleToString(MemberRoleEnum role){
        return switch (role) {
            case MemberRoleEnum.ADMIN -> "ADMIN";
            case MemberRoleEnum.COUNCIL -> "COUNCIL";
            case MemberRoleEnum.COMPANY -> "COMPANY";
            default -> "STUDENT";
        };
    }
}
