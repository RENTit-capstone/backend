package com.capstone.rentit.login.controller;

import com.capstone.rentit.login.dto.JwtTokens;
import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private MemberDetailsService memberDetailsService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @DisplayName("로그인 성공")
    @Test
    void login_success() throws Exception {
        // given
        String email = "test@example.com";
        String rawPw = "password";

        when(memberService.findByEmail(email))
                .thenReturn(Optional.of(mock(Student.class)));

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, rawPw);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authToken);

        when(tokenProvider.generateToken(authToken))
                .thenReturn("ACCESS_TOKEN");
        when(tokenProvider.generateRefreshToken(authToken))
                .thenReturn("REFRESH_TOKEN");

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(rawPw);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("ACCESS_TOKEN"))
                .andExpect(jsonPath("$.data.refreshToken").value("REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("login_success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @DisplayName("로그인 실패 - 미등록 이메일")
    @Test
    void login_unregistered_email() throws Exception {
        // given
        String email = "nouser@example.com";
        when(memberService.findByEmail(email))
                .thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("whatever");

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("등록되지 않은 이메일입니다."));
    }

    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    @Test
    void login_wrong_password() throws Exception {
        // given
        String email = "test2@example.com";
        when(memberService.findByEmail(email))
                .thenReturn(Optional.of(mock(Student.class)));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException(""));

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("wrongpw");

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("accessToken validation error."));
    }

    @DisplayName("토큰 리프레시 성공")
    @Test
    void refresh_success() throws Exception {
        // given
        String email = "refresh@example.com";
        String oldRefresh = "OLD_REFRESH_TOKEN";

        when(tokenProvider.validateRefreshToken(oldRefresh))
                .thenReturn(true);
        when(tokenProvider.getUsernameFromRefreshToken(oldRefresh))
                .thenReturn(email);

        UserDetails user = mock(UserDetails.class);
        when(user.getAuthorities()).thenReturn(null);
        when(memberDetailsService.loadUserByUsername(email))
                .thenReturn(user);

        when(tokenProvider.generateToken(any(Authentication.class)))
                .thenReturn("NEW_ACCESS");
        when(tokenProvider.generateRefreshToken(any(Authentication.class)))
                .thenReturn("NEW_REFRESH");

        JwtTokens reqTokens = new JwtTokens();
        reqTokens.setAccessToken("unused");
        reqTokens.setRefreshToken(oldRefresh);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("NEW_ACCESS"))
                .andExpect(jsonPath("$.data.refreshToken").value("NEW_REFRESH"))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("login_refresh_success",
                        requestFields(
                                fieldWithPath("accessToken").type(JsonFieldType.STRING).description("기존 액세스 토큰 (미사용)"),
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 리프레시 토큰"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @DisplayName("토큰 리프레시 실패 - 잘못된 리프레시 토큰")
    @Test
    void refresh_invalid_token() throws Exception {
        // given
        JwtTokens reqTokens = new JwtTokens();
        reqTokens.setAccessToken("any");
        reqTokens.setRefreshToken("BAD_TOKEN");

        when(tokenProvider.validateRefreshToken("BAD_TOKEN"))
                .thenReturn(false);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("refreshToken validation error."));
    }
}