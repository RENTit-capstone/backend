package com.capstone.rentit.item.status;

public enum ItemStatusEnum {
    OUT, AVAILABLE;

    public static ItemStatusEnum integerToItemStatusEnum(int status){
        if(status == 0){
            return ItemStatusEnum.OUT;
        }
        else if(status == 1){
            return ItemStatusEnum.AVAILABLE;
        }
        else return null;
    }
}
