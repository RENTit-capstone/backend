package com.capstone.rentit.member.controller;

import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.MemberDtoFactory;
import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 신규 회원 생성 (DTO만 입력 받음)
    @PostMapping("/admin/members")
    public MemberDto createMember(@RequestBody MemberCreateForm createForm) {
        Long id = memberService.createMember(createForm);
        return memberService.getUser(id)
                .map(MemberDtoFactory::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 전체 회원 조회 (DTO 목록 반환)
    @GetMapping("/admin/members")
    public List<MemberDto> getAllMembers() {
        return memberService.getAllUsers().stream()
                .map(MemberDtoFactory::toDto)
                .collect(Collectors.toList());
    }

    // 특정 회원 조회
    @GetMapping("/members/{id}")
    public MemberDto getMember(@PathVariable Long id) {
        return memberService.getUser(id)
                .map(MemberDtoFactory::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 업데이트: MemberUpdateForm을 받아 업데이트 수행
    @PutMapping("/members/{id}")
    public MemberDto updateMember(@PathVariable Long id, @RequestBody MemberUpdateForm updateForm) {
        return MemberDtoFactory.toDto(memberService.updateUser(id, updateForm));
    }

    // 회원 삭제
    @DeleteMapping("/admin/members/{id}")
    public void deleteMember(@PathVariable Long id) {
        memberService.deleteUser(id);
    }

    @GetMapping("/members/me")
    public MemberDto getLoginMember(@Login MemberDto memberDto) {
        // 로그인된 사용자의 타입에 따라 StudentDto, CompanyDto, StudentCouncilMemberDto 중 하나가 주입
        return memberDto;
    }
}
