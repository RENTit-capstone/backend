package com.capstone.rentit.otp.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/otp")
    public CommonResponse<?> requestOtp(@Login MemberDto memberDto) {
        String code = otpService.generateOtp(memberDto.getEmail());
        return CommonResponse.success(code);
    }
}
