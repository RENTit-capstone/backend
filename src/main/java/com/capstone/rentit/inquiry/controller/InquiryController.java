package com.capstone.rentit.inquiry.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.inquiry.dto.*;
import com.capstone.rentit.inquiry.service.InquiryService;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/inquiries")
    public CommonResponse<Long> create(
            @Login MemberDto memberDto,
            @RequestBody InquiryCreateForm form) {
        return CommonResponse.success(inquiryService.createInquiry(memberDto.getMemberId(), form));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/inquiries/damage")
    public CommonResponse<Long> createDamageReport(
            @Login MemberDto memberDto,
            @RequestBody DamageReportCreateForm form) {
        return CommonResponse.success(inquiryService.createDamageReport(memberDto.getMemberId(), form));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/inquiries/{id}/answer")
    public CommonResponse<Void> answerDamageReport(
            @Login MemberDto memberDto,
            @PathVariable("id") Long id,
            @RequestBody InquiryAnswerForm form) {

        inquiryService.answerDamageReport(id, memberDto.getMemberId(), form);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/inquiries/{id}")
    public CommonResponse<InquiryResponse> findInquiry(@PathVariable Long id) {
        return CommonResponse.success(inquiryService.getInquiry(id));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/inquiries")
    public CommonResponse<?> search(
            @Login MemberDto memberDto,
            @ModelAttribute("form") InquirySearchForm form,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return CommonResponse.success(inquiryService.search(form, memberDto.getRole(), memberDto.getMemberId(), pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/inquiries/{id}")
    public CommonResponse<InquiryResponse> findInquiryForAdmin(@PathVariable Long id) {
        return CommonResponse.success(inquiryService.getInquiry(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/inquiries")
    public CommonResponse<Page<InquiryResponse>> searchForAdmin(
            @Login MemberDto memberDto,
            @ModelAttribute("form") InquirySearchForm form,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return CommonResponse.success(inquiryService.search(form, memberDto.getRole(), memberDto.getMemberId(), pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/admin/inquiries/{id}/answer")
    public CommonResponse<Long> answerInquiry(
            @PathVariable("id") Long id,
            @RequestBody InquiryAnswerForm form
    ) {
        inquiryService.answerInquiry(id, form);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/inquiries/{id}/processed")
    public CommonResponse<Void> markProcessed(@PathVariable Long id) {
        inquiryService.markProcessed(id);
        return CommonResponse.success(null);
    }
}