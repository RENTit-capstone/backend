package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.member.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MemberExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public CommonResponse<Void> handleNotFound(MemberNotFoundException ex) {
        log.warn("Member not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(UnsupportedMemberTypeException.class)
    public CommonResponse<Void> handleUnsupportedType(UnsupportedMemberTypeException ex) {
        log.warn("Unsupported member action: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(MemberTypeMismatchException.class)
    public CommonResponse<Void> handleTypeMismatch(MemberTypeMismatchException ex) {
        log.warn("Member action mismatch: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }
}