package com.capstone.rentit.login.controller;

import com.capstone.rentit.login.dto.JwtTokens;
import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.member.status.GenderEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 로그인 성공 케이스 테스트
     */
    @Test
    public void login_success() throws Exception {
        // 더미 계정 등록
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender(GenderEnum.MEN);
        form.setPhone("010-1234-5678");

        memberService.createMember(form);

        // (1) 로그인 요청
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("login-success",
                        requestFields(
                                fieldWithPath("email").description("이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("비밀번호").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.accessToken").description("Access Token").type(JsonFieldType.STRING),
                                fieldWithPath("data.refreshToken").description("Refresh Token").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * 로그인 실패 - 미등록 이메일 테스트 케이스
     */
    @Test
    public void login_unregistered_email() throws Exception {
        // 등록되지 않은 이메일로 로그인 요청
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("password");

        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("등록되지 않은 이메일입니다."));
    }

    /**
     * 로그인 실패 - 잘못된 비밀번호 테스트 케이스
     */
    @Test
    public void login_wrong_password() throws Exception {
        // 더미 계정 등록
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Test User2");
        form.setEmail("test2@example.com");
        form.setPassword("password2");
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender(GenderEnum.MEN);
        form.setPhone("010-1234-5678");

        memberService.createMember(form);

        // (1) 로그인 요청
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test2@example.com");
        loginRequest.setPassword("password");

        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("accessToken validation error."));
    }

    @Test
    public void refresh_success() throws Exception {
        // 더미 계정 등록
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Refresh User");
        form.setEmail("refresh@example.com");
        form.setPassword("password");
        form.setNickname("refreshTester");
        form.setUniversity("Test University");
        form.setStudentId("87654321");
        form.setGender(GenderEnum.WOMEN);
        form.setPhone("010-5678-1234");

        memberService.createMember(form);

        // 먼저 로그인해서 access, refresh 토큰을 발급받음
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("refresh@example.com");
        loginRequest.setPassword("password");
        String loginJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginNode = objectMapper.readTree(loginResponse);
        JsonNode dataNode = loginNode.path("data");
        String refreshToken = dataNode.path("refreshToken").asText();

        // refresh 토큰을 이용해 새 토큰 재발급 요청
        JwtTokens refreshRequest = new JwtTokens();
        // accessToken은 사용하지 않으므로 임의의 값
        refreshRequest.setAccessToken("dummy");
        refreshRequest.setRefreshToken(refreshToken);
        String refreshJson = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/v1/auth/login/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("login-refresh-success",
                requestFields(
                        fieldWithPath("accessToken").description("accessToken").type(JsonFieldType.STRING),
                        fieldWithPath("refreshToken").description("refreshToken").type(JsonFieldType.STRING)
                ),
                responseFields(
                        fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                        fieldWithPath("data.accessToken").description("Access Token").type(JsonFieldType.STRING),
                        fieldWithPath("data.refreshToken").description("Refresh Token").type(JsonFieldType.STRING),
                        fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                )
        ));
    }

    /**
     * refresh 토큰 재발급 실패 테스트 케이스 (유효하지 않은 refresh 토큰)
     */
    @Test
    public void refresh_invalid_token() throws Exception {
        // 유효하지 않은 refresh 토큰 사용
        JwtTokens refreshRequest = new JwtTokens();
        refreshRequest.setAccessToken("dummy");
        refreshRequest.setRefreshToken("invalid-token");
        String refreshJson = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/v1/auth/login/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("refreshToken validation error."));
    }
}