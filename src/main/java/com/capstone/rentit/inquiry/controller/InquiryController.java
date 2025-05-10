package com.capstone.rentit.inquiry.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.inquiry.dto.InquiryCreateForm;
import com.capstone.rentit.inquiry.dto.InquiryResponse;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
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
    public CommonResponse<Long> create(@RequestBody InquiryCreateForm form) {
        return CommonResponse.success(inquiryService.createInquiry(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/inquiries/{id}")
    public CommonResponse<InquiryResponse> findInquiryForAdmin(@PathVariable Long id) {
        return CommonResponse.success(inquiryService.getInquiry(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/inquiries")
    public CommonResponse<Page<InquiryResponse>> searchForAdmin(
            @ModelAttribute("form") InquirySearchForm form,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return CommonResponse.success(inquiryService.search(form, pageable));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/inquiries/{id}")
    public CommonResponse<InquiryResponse> findInquiry(@PathVariable Long id) {
        return CommonResponse.success(inquiryService.getInquiry(id));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/inquiries")
    public CommonResponse<List<InquiryResponse>> search(
            @Login MemberDto memberDto,
            @ModelAttribute("type") InquiryType type) {
        return CommonResponse.success(inquiryService.getInquiries(memberDto.getMemberId(), type));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/inquiries/{id}/processed")
    public CommonResponse<Void> markProcessed(@PathVariable Long id) {
        inquiryService.markProcessed(id);
        return CommonResponse.success(null);
    }
}