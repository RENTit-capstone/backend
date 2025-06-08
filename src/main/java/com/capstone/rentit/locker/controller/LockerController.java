package com.capstone.rentit.locker.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.service.LockerService;
import jakarta.validation.Valid;
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
    @PostMapping("/admin/devices")
    public CommonResponse<?> registerDevice(@RequestBody @Valid DeviceCreateForm form) {
        return CommonResponse.success(lockerService.registerDevice(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/devices")
    public CommonResponse<List<DeviceResponse>> listDevices(@ModelAttribute DeviceSearchForm form) {
        return CommonResponse.success(lockerService.searchDevicesByUniversity(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/lockers")
    public CommonResponse<?> registerLocker(@RequestBody @Valid LockerCreateForm form) {
        return CommonResponse.success(lockerService.registerLocker(form));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/lockers")
    public CommonResponse<List<LockerResponse>> listLockers(@ModelAttribute LockerSearchForm form) {
        return CommonResponse.success(lockerService.searchLockers(form));
    }

}
