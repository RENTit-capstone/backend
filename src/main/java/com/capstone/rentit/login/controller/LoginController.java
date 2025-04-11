package com.capstone.rentit.login.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;

    @PostMapping("/auth/login")
    public CommonResponse<String> authenticateUser(@RequestBody LoginRequest loginRequest) {
        if (memberService.findByEmail(loginRequest.getEmail()).isEmpty()) {
            return new CommonResponse<>(false, null, "등록되지 않은 이메일입니다.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            String jwt = tokenProvider.generateToken(authentication);
            return new CommonResponse<>(true, jwt, "");
        } catch (Exception ex) {
            return new CommonResponse<>(false, null, "비밀번호가 일치하지 않습니다.");
        }
    }
}
