package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.item.exception.ItemImageMissingException;
import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.exception.ItemUnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ItemExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public CommonResponse<Void> handleNotFound(ItemNotFoundException ex) {
        log.warn("Item not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(ItemUnauthorizedException.class)
    public CommonResponse<Void> handleUnauthorized(ItemUnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(ItemImageMissingException.class)
    public CommonResponse<Void> handleMissedImage(ItemImageMissingException ex) {
        log.warn("Item don't have images: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

}
