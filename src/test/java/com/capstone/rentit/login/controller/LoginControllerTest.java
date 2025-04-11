package com.capstone.rentit.login.controller;

import com.capstone.rentit.login.dto.LoginRequest;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.register.dto.StudentRegisterForm;
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
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setRole(1);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        memberService.createStudent(form);

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
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("login_success",
                        requestFields(
                                fieldWithPath("email").description("이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("비밀번호").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("JWT 토큰").type(JsonFieldType.STRING),
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
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("등록되지 않은 이메일입니다."));
    }

    /**
     * 로그인 실패 - 잘못된 비밀번호 테스트 케이스
     */
    @Test
    public void login_wrong_password() throws Exception {
        // 더미 계정 등록
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User2");
        form.setEmail("test2@example.com");
        form.setPassword("password2");
        form.setRole(1);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        memberService.createStudent(form);

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
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
        }
}