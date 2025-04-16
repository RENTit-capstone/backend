package com.capstone.rentit.common;

public class ItemStatusConverter {

    public static ItemStatusEnum fromInteger(Integer status) {
        return switch (status) {
            case 0 -> ItemStatusEnum.OUT;
            default ->
                    ItemStatusEnum.AVAILABLE;
        };
    }

    public static String statusToString(ItemStatusEnum role){
        return switch (role) {
            case ItemStatusEnum.OUT -> "OUT";
            default -> "AVAILABLE";
        };
    }
}