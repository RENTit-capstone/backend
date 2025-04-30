package com.capstone.rentit.login.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.dto.JwtTokens;
import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final MemberDetailsService memberDetailsService;

    @PostMapping("/auth/login")
    public CommonResponse<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        memberService.getMemberByEmail(loginRequest.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            return new CommonResponse<>(true, new JwtTokens(accessToken, refreshToken), "");
        } catch (Exception ex) {
            return new CommonResponse<>(false, null, "accessToken validation error.");
        }
    }

    @PostMapping("/auth/login/refresh")
    public CommonResponse<?> refreshToken(@RequestBody JwtTokens tokens) {
        String refreshToken = tokens.getRefreshToken();

        if (refreshToken == null || !tokenProvider.validateRefreshToken(refreshToken)) {
            return new CommonResponse<>(false, null, "refreshToken validation error.");

        }

        // refresh 토큰에서 username 추출 후, 해당 사용자 정보를 로드
        String username = tokenProvider.getUsernameFromRefreshToken(refreshToken);
        UserDetails userDetails = memberDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return new CommonResponse<>(true, new JwtTokens(newAccessToken, newRefreshToken), "");

    }
}
