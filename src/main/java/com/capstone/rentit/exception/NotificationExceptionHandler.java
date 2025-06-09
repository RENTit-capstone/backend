package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.notification.exception.NotificationAccessDenied;
import com.capstone.rentit.notification.exception.NotificationNotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFound.class)
    public CommonResponse<Void> handleNotFound(NotificationNotFound ex) {
        log.warn("Notification not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(NotificationAccessDenied.class)
    public CommonResponse<Void> handleNotFound(NotificationAccessDenied ex) {
        log.warn("Notification access denied: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }
}
