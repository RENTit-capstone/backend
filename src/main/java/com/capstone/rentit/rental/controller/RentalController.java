package com.capstone.rentit.rental.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /** 1) 대여 요청 */
    @PostMapping("/rentals")
    public CommonResponse<Long> requestRental(@RequestBody RentalRequestForm form) {
        Long id = rentalService.requestRental(form);
        return CommonResponse.success(id);
    }

    /** 2) 내 대여 목록 조회 (소유자·대여자) */
    @GetMapping("/rentals")
    public CommonResponse<List<RentalDto>> getMyRentals(@Login MemberDto loginMember) {
        List<RentalDto> list = rentalService.getRentalsForUser(loginMember);
        return CommonResponse.success(list);
    }

    /** 3) 단일 대여 조회 */
    @GetMapping("/rentals/{rentalId}")
    public CommonResponse<RentalDto> getRental(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        RentalDto dto = rentalService.getRental(rentalId, loginMember);
        return CommonResponse.success(dto);
    }

    /** 4) 대여 승인 (소유자/관리자) */
    @PostMapping("/rentals/{rentalId}/approve")
    public CommonResponse<Void> approveRental(@PathVariable Long rentalId) {
        rentalService.approve(rentalId);
        return CommonResponse.success(null);
    }

    /** 5) 대여 거절 (소유자/관리자) */
    @PostMapping("/rentals/{rentalId}/reject")
    public CommonResponse<Void> rejectRental(@PathVariable Long rentalId) {
        rentalService.reject(rentalId);
        return CommonResponse.success(null);
    }

    /** 6) 대여 취소 (반드시 대여자만) */
    @PostMapping("/rentals/{rentalId}/cancel")
    public CommonResponse<Void> cancelRental(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.cancel(rentalId, loginMember.getId());
        return CommonResponse.success(null);
    }

    /** 7) 소유자가 사물함에 물건 맡기기 */
    @PostMapping("/rentals/{rentalId}/dropoff")
    public CommonResponse<Void> dropOffToLocker(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.dropOffToLocker(rentalId, loginMember.getId());
        return CommonResponse.success(null);
    }

    /** 8) 대여자가 사물함에서 픽업 */
    @PostMapping("/rentals/{rentalId}/pickup")
    public CommonResponse<Void> pickUpByRenter(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.pickUpByRenter(rentalId, loginMember.getId());
        return CommonResponse.success(null);
    }

    /** 9) 대여자가 사물함에 반납 */
    @PostMapping("/rentals/{rentalId}/return")
    public CommonResponse<Void> returnToLocker(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.returnToLocker(rentalId, loginMember.getId());
        return CommonResponse.success(null);
    }

    /** 10) 소유자가 사물함에서 회수 (대여 완료) */
    @PostMapping("/rentals/{rentalId}/retrieve")
    public CommonResponse<Void> retrieveByOwner(
            @PathVariable Long rentalId,
            @Login MemberDto loginMember
    ) {
        rentalService.retrieveByOwner(rentalId, loginMember.getId());
        return CommonResponse.success(null);
    }

    /** 관리자용: 특정 사용자 대여 목록 조회 */
    @GetMapping("/admin/rentals/{userId}")
    public CommonResponse<List<RentalDto>> getRentalsByUser(@PathVariable Long userId) {
        List<RentalDto> list = rentalService.getRentalsByUser(userId);
        return CommonResponse.success(list);
    }
}
