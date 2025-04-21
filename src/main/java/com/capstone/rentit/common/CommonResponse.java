package com.capstone.rentit.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, "");
    }

    public static <T> CommonResponse<T> failure(String message) {
        return new CommonResponse<>(false, null, message);
    }
}
