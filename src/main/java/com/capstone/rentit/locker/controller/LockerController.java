package com.capstone.rentit.locker.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.service.LockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/lockers")
    public CommonResponse<?> register(@RequestBody LockerCreateForm form) {
        return CommonResponse.success(lockerService.registerLocker(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/lockers")
    public CommonResponse<List<LockerDto>> list(@ModelAttribute LockerSearchForm form) {
        return CommonResponse.success(lockerService.searchLockers(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/lockers/{lockerId}")
    public CommonResponse<LockerDto> get(@PathVariable Long lockerId) {
        return CommonResponse.success(lockerService.getLocker(lockerId));
    }
}
