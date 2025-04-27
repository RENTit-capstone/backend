package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return CommonResponse.failure("서버 오류가 발생했습니다.");
    }
}
