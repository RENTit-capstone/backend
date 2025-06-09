package com.capstone.rentit.rental.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.dto.RentalBriefResponse;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.dto.RentalSearchForm;
import com.capstone.rentit.rental.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /** 1) 대여 요청 */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/rentals")
    public CommonResponse<Long> requestRental(@RequestBody @Valid RentalRequestForm form) {
        Long id = rentalService.requestRental(form);
        return CommonResponse.success(id);
    }

    /** 2) 내 대여 목록 조회 (소유자·대여자) */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/rentals")
    public CommonResponse<Page<RentalDto>> getMyRentals(
            @Login MemberDto loginMember,
            @ModelAttribute RentalSearchForm searchForm,
            @PageableDefault(size = 20, sort = "requestDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<RentalDto> page = rentalService.getRentalsForUser(loginMember, searchForm, pageable);
        return CommonResponse.success(page);
    }

    /** 3) 단일 대여 조회 */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/rentals/{rentalId}")
    public CommonResponse<RentalDto> getRental(
            @PathVariable("rentalId") Long rentalId,
            @Login MemberDto loginMember
    ) {
        RentalDto dto = rentalService.getRental(rentalId, loginMember.getMemberId());
        return CommonResponse.success(dto);
    }

    /** 4) 대여 승인 (소유자) */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/rentals/{rentalId}/approve")
    public CommonResponse<Void> approveRental(@PathVariable("rentalId") Long rentalId) {
        rentalService.approve(rentalId);
        return CommonResponse.success(null);
    }

    /** 5) 대여 거절 (소유자) */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/rentals/{rentalId}/reject")
    public CommonResponse<Void> rejectRental(@PathVariable("rentalId") Long rentalId) {
        rentalService.reject(rentalId);
        return CommonResponse.success(null);
    }

    /** 6) 대여 취소 (반드시 대여자만) */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/rentals/{rentalId}/cancel")
    public CommonResponse<Void> cancelRental(
            @PathVariable("rentalId") Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.cancel(rentalId, loginMember.getMemberId());
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(path = "/rentals/{rentalId}/return-image")
    public CommonResponse<Void> uploadReturnImage(
            @Login MemberDto memberDto,
            @PathVariable Long rentalId,
            @RequestParam("returnImageKey") String returnImageKey) {
        rentalService.uploadReturnImage(rentalId, memberDto.getMemberId(), returnImageKey);
        return CommonResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/rentals/{userId}")
    public CommonResponse<List<RentalBriefResponse>> getRentalsByUser(@PathVariable("userId") Long userId) {
        List<RentalBriefResponse> list = rentalService.getRentalsByUser(userId);
        return CommonResponse.success(list);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/rentals")
    public CommonResponse<Page<RentalBriefResponse>> getAllRentals(
            @ModelAttribute RentalSearchForm searchForm,
            @PageableDefault(size = 20, sort = "requestDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<RentalBriefResponse> page = rentalService.getAllRentals(searchForm, pageable);
        return CommonResponse.success(page);
    }
}
