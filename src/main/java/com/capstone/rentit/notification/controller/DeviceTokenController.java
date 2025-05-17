package com.capstone.rentit.notification.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.notification.dto.TokenRequest;
import com.capstone.rentit.notification.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device-token")
public class DeviceTokenController {

    private final DeviceTokenService tokenService;

    @PostMapping
    public CommonResponse<?> registerToken(@RequestBody TokenRequest dto,
                                        @Login MemberDto memberDto) {
        tokenService.saveToken(memberDto.getMemberId(), dto.token());
        return CommonResponse.success(null);
    }
}

