package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.inquiry.exception.InquiryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InquiryExceptionHandler {

    @ExceptionHandler(InquiryNotFoundException.class)
    public CommonResponse<Void> handleNotFound(InquiryNotFoundException ex) {
        log.warn("Inquiry not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }
}
