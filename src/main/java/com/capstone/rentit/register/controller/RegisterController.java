package com.capstone.rentit.register.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.otp.service.OtpService;
import com.capstone.rentit.register.dto.RegisterVerifyCodeForm;
import com.capstone.rentit.register.dto.RegisterVerifyRequestForm;
import com.capstone.rentit.register.service.EmailService;
import com.capstone.rentit.register.service.UnivCertService;
import com.capstone.rentit.register.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class RegisterController {

    private final MemberService memberService;
    private final VerificationService verificationService;

    @PostMapping("/auth/signup/group")
    public CommonResponse<Long> registerGroupMember(@RequestBody MemberCreateForm form) {
        Long memberId = memberService.createMember(form);
        return CommonResponse.success(memberId);
    }

    @PostMapping("/auth/signup")
    public CommonResponse<Long> registerMember(@RequestBody MemberCreateForm form) {
        boolean succeed = false;
        try {
            memberService.ensureEmailNotRegistered(form.getEmail());
            verificationService.ensureEmailVerified(form.getEmail());

            Long memberId = memberService.createMember(form);

            succeed = true;
            return CommonResponse.success(memberId);
        } finally {
            if(!succeed)
                verificationService.clearVerification(form.getEmail());
        }

    }

    @PostMapping("/auth/signup/verify-email")
    public CommonResponse<String> verifyRequest(@RequestBody RegisterVerifyRequestForm form) {

        verificationService.generateAndSendVerificationCode(form.getEmail());
        return CommonResponse.success("이메일로 발송된 인증 코드를 확인하세요.");
    }

    @PostMapping("/auth/signup/verify-code")
    public CommonResponse<Boolean> verifyCode(@RequestBody RegisterVerifyCodeForm form) {

        verificationService.verifyCode(form.getEmail(), form.getCode());
        return CommonResponse.success(true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/auth/signup/{id}")
    public CommonResponse<Boolean> deleteUser(@PathVariable("id") Long id) {
        memberService.deleteMember(id);
        return CommonResponse.success(true);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/auth/signup/clear")
    public CommonResponse<Boolean> clearAll() {
        verificationService.clearAllVerifications();
        return CommonResponse.success(true);
    }
}
