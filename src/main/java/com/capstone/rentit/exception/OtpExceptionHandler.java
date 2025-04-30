package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.otp.exception.OtpExpiredException;
import com.capstone.rentit.otp.exception.OtpMismatchException;
import com.capstone.rentit.otp.exception.OtpNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OtpExceptionHandler {

    @ExceptionHandler(OtpNotFoundException.class)
    public CommonResponse<Void> handleOtpNotFound(OtpNotFoundException ex) {
        log.warn("OTP not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(OtpExpiredException.class)
    public CommonResponse<Void> handleOtpExpired(OtpExpiredException ex) {
        log.info("OTP expired: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(OtpMismatchException.class)
    public CommonResponse<Void> handleOtpMismatch(OtpMismatchException ex) {
        log.info("OTP mismatch: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }
}
