package com.capstone.rentit.exception;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.register.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegisterExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public CommonResponse<Void> handleEmailDup(EmailAlreadyRegisteredException ex) {
        log.warn(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(NicknameAlreadyRegisteredException.class)
    public CommonResponse<Void> handleNicknameDup(NicknameAlreadyRegisteredException ex) {
        log.warn(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(UnivNotCertifiedException.class)
    public CommonResponse<Void> handleNotCertified(UnivNotCertifiedException ex) {
        log.warn(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(InvalidUniversityException.class)
    public CommonResponse<Void> handleInvalidUniv(InvalidUniversityException ex) {
        log.warn(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(CertificationSendFailureException.class)
    public CommonResponse<Void> handleSendFail(CertificationSendFailureException ex) {
        log.error(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public CommonResponse<Void> handleInvalidCode(InvalidVerificationCodeException ex) {
        log.info(ex.getMessage());
        return CommonResponse.failure(ex.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public CommonResponse<Void> handleMemberNotFound(MemberNotFoundException ex) {
        log.warn(ex.getMessage());
        return CommonResponse.failure("사용자를 찾을 수 없습니다.");
    }

    @ExceptionHandler(UnivServiceException.class)
    public CommonResponse<Void> handleServiceError(UnivServiceException ex) {
        log.error(ex.getMessage(), ex);
        return CommonResponse.failure(ex.getMessage());
    }
}
