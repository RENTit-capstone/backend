package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.payment.exception.ExternalPaymentFailedException;
import com.capstone.rentit.payment.exception.InsufficientBalanceException;
import com.capstone.rentit.payment.exception.PaymentNotLockerException;
import com.capstone.rentit.payment.exception.WalletNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    @ExceptionHandler(ExternalPaymentFailedException.class)
    public CommonResponse<Void> handleExternalPaymentFailed(ExternalPaymentFailedException ex) {
        log.warn("External payment failed: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public CommonResponse<Void> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient Balance: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(PaymentNotLockerException.class)
    public CommonResponse<Void> handleNotLocker(PaymentNotLockerException ex) {
        log.warn("Payment Not Locker: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public CommonResponse<Void> handleNotFound(WalletNotFoundException ex) {
        log.warn("Wallet Not found: {}", ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }
}
