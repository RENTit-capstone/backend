package com.capstone.rentit.member.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.MemberDtoFactory;
import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.service.MemberService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 관리자용 신규 회원 생성
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/members")
    public CommonResponse<?> createMember(@RequestBody MemberCreateForm createForm) {
        Long id = memberService.createMember(createForm);
        return CommonResponse.success(id);
    }

    // 전체 회원 조회 (DTO 목록 반환)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/members")
    public CommonResponse<List<MemberDto>> getAllMembers() {
        List<MemberDto> list = memberService.getAllUsers().stream()
                .map(MemberDtoFactory::toDto)
                .collect(Collectors.toList());
        return CommonResponse.success(list);
    }

    // 특정 회원 조회
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/members/{id}")
    public CommonResponse<MemberDto> getMember(@PathVariable("id") Long id) {
        Optional<MemberDto> optionalDto = memberService.getUser(id)
                .map(MemberDtoFactory::toDto);
        return optionalDto.map(CommonResponse::success).orElseGet(() -> CommonResponse.failure("존재하지 않는 사용자"));
    }

    // 업데이트: MemberUpdateForm을 받아 업데이트 수행
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/members")
    public CommonResponse<?> updateMember(@Login MemberDto loginMember,
                                          @RequestBody MemberUpdateForm updateForm) {
        memberService.updateUser(loginMember.getId(), updateForm);
        return CommonResponse.success(null);
    }

    // 회원 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/members/{id}")
    public CommonResponse<?> deleteMember(@PathVariable("id") Long id) {
        memberService.deleteUser(id);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/members/me")
    public CommonResponse<MemberDto> getLoginMember(@Login MemberDto memberDto) {
        return CommonResponse.success(memberDto);
    }
}
