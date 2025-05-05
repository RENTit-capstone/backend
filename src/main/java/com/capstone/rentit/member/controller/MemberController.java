package com.capstone.rentit.member.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.dto.MyProfileResponse;
import com.capstone.rentit.member.service.MemberService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
        List<MemberDto> memberDtos = memberService.getAllMembers();
        return CommonResponse.success(memberDtos);
    }

    // 특정 회원 조회
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/members/{id}")
    public CommonResponse<MemberDto> getMember(@PathVariable("id") Long id) {
        MemberDto memberDto = memberService.getMemberById(id);
        return CommonResponse.success(memberDto);
    }

    // 업데이트: MemberUpdateForm을 받아 업데이트 수행
    @PreAuthorize("hasRole('USER')")
    @PutMapping(path = "/members",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<?> updateMember(@Login MemberDto loginMember,
                                          @RequestPart(value = "form", required = false) MemberUpdateForm form,
                                          @RequestPart(value = "image", required = false) MultipartFile image) {
        memberService.updateMember(loginMember.getMemberId(), form, image);
        return CommonResponse.success(null);
    }

    // 회원 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/members/{id}")
    public CommonResponse<?> deleteMember(@PathVariable("id") Long id) {
        memberService.deleteMember(id);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/members/me")
    public CommonResponse<MyProfileResponse> getLoginMember(@Login MemberDto memberDto) {
        MyProfileResponse myProfileResponse = memberService.getMyProfile(memberDto.getMemberId());
        return CommonResponse.success(myProfileResponse);
    }
}
