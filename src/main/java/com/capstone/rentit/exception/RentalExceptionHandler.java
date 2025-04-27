package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.rental.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RentalExceptionHandler {

    @ExceptionHandler({ RentalNotFoundException.class, ItemNotFoundException.class })
    public CommonResponse<Void> handleNotFound(RuntimeException ex) {
        log.warn("Not found error: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(ItemAlreadyRentedException.class)
    public CommonResponse<Void> handleAlreadyRented(ItemAlreadyRentedException ex) {
        log.info("Already rented: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(RentalUnauthorizedException.class)
    public CommonResponse<Void> handleUnauthorized(RentalUnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(ReturnImageMissingException.class)
    public CommonResponse<Void> handleReturnImageMissing(ReturnImageMissingException ex) {
        log.info("Missing return image: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

}