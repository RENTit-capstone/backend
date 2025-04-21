package com.capstone.rentit.rental.controller;

import com.capstone.rentit.common.CommonResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RentalControllerAdvice {

    /** 잘못된 요청(파라미터 등)에 대한 처리 */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResponse<Void> handleBadRequest(IllegalArgumentException ex) {
        return CommonResponse.failure(ex.getMessage());
    }

    /** 권한 오류 처리 */
    @ExceptionHandler(SecurityException.class)
    public CommonResponse<Void> handleForbidden(SecurityException ex) {
        return CommonResponse.failure(ex.getMessage());
    }

    /** 그 외 예기치 못한 서버 오류 처리 */
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleOther(Exception ex) {
        return CommonResponse.failure("서버 오류가 발생했습니다.");
    }
}