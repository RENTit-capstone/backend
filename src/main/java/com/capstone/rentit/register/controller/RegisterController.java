package com.capstone.rentit.register.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.register.dto.RegisterVerifyCodeForm;
import com.capstone.rentit.register.dto.RegisterVerifyRequestForm;
import com.capstone.rentit.register.service.UnivCertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class RegisterController {
    private final MemberService memberService;
    private final UnivCertService univCertService;

    @PostMapping("/auth/signup")
    public CommonResponse<Long> registerMember(@RequestBody StudentRegisterForm form) {
        Optional<Member> existingUser = memberService.findByEmail(form.getEmail());
        if (existingUser.isPresent()) {
            return CommonResponse.failure("이미 등록된 이메일입니다.");
        }
        if (!univCertService.isCertified(form.getEmail())) {
            return CommonResponse.failure("미인증 이메일입니다.");
        }

        Long memberId = memberService.createMember(form);
        return CommonResponse.success(memberId);
    }

    @PostMapping("/auth/signup/verify-email")
    public CommonResponse<String> verifyRequest(@RequestBody RegisterVerifyRequestForm requestForm) {
        boolean isValidUniv = univCertService.checkUniversity(requestForm.getUniversity());
        if (!isValidUniv) {
            return CommonResponse.failure("유효하지 않은 대학명 또는 상위 150개 대학에 포함되지 않습니다.");
        }
        boolean certifySent = univCertService.certify(requestForm.getEmail(), requestForm.getUniversity(), false);
        if (!certifySent) {
            return CommonResponse.failure("인증 코드 발송에 실패했습니다.");
        }
        return CommonResponse.success("이메일로 발송된 인증 코드를 확인하세요.");
    }

    @PostMapping("/auth/signup/verify-code")
    public CommonResponse<Boolean> verifyCode(@RequestBody RegisterVerifyCodeForm codeForm) {
        boolean isValidUniv = univCertService.checkUniversity(codeForm.getUniversity());
        if (!isValidUniv) {
            return CommonResponse.failure("유효하지 않은 대학명 또는 상위 150개 대학에 포함되지 않습니다.");
        }
        boolean isVerified = univCertService.certifyCode(codeForm.getEmail(), codeForm.getUniversity(), codeForm.getCode());
        if (isVerified) {
            return CommonResponse.success(true);
        } else {
            return CommonResponse.failure("잘못된 인증 코드입니다.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/auth/signup/{id}")
    public CommonResponse<Boolean> deleteUser(@PathVariable("id") Long id) {
        try {
            memberService.deleteUser(id);
            return CommonResponse.success(true);
        } catch (RuntimeException e) {
            return CommonResponse.failure("사용자를 찾을 수 없습니다.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/auth/signup/clear")
    public CommonResponse<Boolean> clearAll() {
        if (univCertService.clearAll()) {
            return CommonResponse.success(true);
        } else {
            return CommonResponse.failure("에러 발생");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/auth/signup/show")
    public CommonResponse<Object> showAll() {
        Object data = univCertService.showAll();
        if ("error".equals(data)) {
            return CommonResponse.failure("에러 발생");
        }
        return CommonResponse.success(data);
    }
}
