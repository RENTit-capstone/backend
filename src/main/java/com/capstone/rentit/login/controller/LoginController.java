package com.capstone.rentit.login.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.dto.JwtTokens;
import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.login.dto.LoginResponse;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final MemberDetailsService memberDetailsService;

    @PostMapping("/auth/login")
    public CommonResponse<?> authenticateUser(@RequestBody @Valid LoginRequest loginRequest) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            MemberDto memberDto = memberService.getMemberByEmail(loginRequest.getEmail());

            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            return CommonResponse.success(new LoginResponse(memberDto.getMemberId(), accessToken, refreshToken));
        } catch (Exception ex) {
            log.info("login error: {}", ex.getMessage());
            return CommonResponse.failure("accessToken validation error.");
        }
    }

    @PostMapping("/auth/login/refresh")
    public CommonResponse<?> refreshToken(@RequestBody JwtTokens tokens) {
        String refreshToken = tokens.getRefreshToken();

        if (refreshToken == null || !tokenProvider.validateRefreshToken(refreshToken)) {
            return CommonResponse.failure("refreshToken validation error.");

        }

        // refresh 토큰에서 username 추출 후, 해당 사용자 정보를 로드
        String username = tokenProvider.getUsername(refreshToken);
        MemberDetails userDetails = (MemberDetails)memberDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return CommonResponse.success(new LoginResponse(userDetails.getMemberId(), newAccessToken, newRefreshToken));

    }

    @PostMapping("/auth/logout")
    public CommonResponse<?> logout(@RequestBody JwtTokens tokens) {

        String refreshToken = tokens.getRefreshToken();
        if (refreshToken != null && tokenProvider.validateRefreshToken(refreshToken)) {
            tokenProvider.revokeRefreshToken(refreshToken);   // Redis에서 키 제거
        }

        SecurityContextHolder.clearContext();

        return CommonResponse.success(null);
    }
}
