package com.capstone.rentit.common;

public class MemberRoleConverter {

    public static MemberRoleEnum fromInteger(Integer roleInt) {
        if (roleInt == null) {
            return MemberRoleEnum.STUDENT;
        }
        return switch (roleInt) {
            case 1 -> MemberRoleEnum.COUNCIL;
            case 2 -> MemberRoleEnum.COMPANY;
            case 3 -> MemberRoleEnum.ADMIN;
            default ->
                    MemberRoleEnum.STUDENT;
        };
    }

    public static String roleToString(MemberRoleEnum role){
        return switch (role) {
            case MemberRoleEnum.ADMIN -> "ADMIN";
            case MemberRoleEnum.COUNCIL -> "COUNCIL";
            case MemberRoleEnum.COMPANY -> "COMPANY";
            default -> "STUDENT";
        };
    }
}