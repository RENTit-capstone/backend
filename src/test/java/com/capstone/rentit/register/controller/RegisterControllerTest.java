package com.capstone.rentit.register.controller;

import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.register.dto.RegisterVerifyCodeForm;
import com.capstone.rentit.register.dto.RegisterVerifyRequestForm;
import com.capstone.rentit.register.dto.StudentRegisterForm;
import com.capstone.rentit.register.service.UnivCertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnivCertService univCertService;

    @MockitoBean
    private MemberService memberService;

    /**
     * 회원 가입 성공 케이스 테스트
     */
    @Test
    public void register_member_success() throws Exception {
        // 요청 데이터 준비
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

        // 이미 등록된 이메일이 아님을 가정
        when(memberService.findByEmail("test@example.com")).thenReturn(Optional.empty());
        // 인증된 이메일임을 가정
        when(univCertService.isCertified("test@example.com")).thenReturn(true);
        // 학생 생성 시 1번 ID가 리턴되는 것을 모킹
        Long generatedId = 1L;
        when(memberService.createStudent(any(StudentRegisterForm.class))).thenReturn(generatedId);

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(generatedId))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("register-member-success",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("사용자 비밀번호").type(JsonFieldType.STRING),
                                fieldWithPath("name").description("사용자 이름").type(JsonFieldType.STRING),
                                fieldWithPath("role").description("사용자 역할 (예: 1은 학생)").type(JsonFieldType.NUMBER),
                                fieldWithPath("nickname").description("사용자 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("사용자 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("gender").description("성별").type(JsonFieldType.STRING),
                                fieldWithPath("phone").description("연락처").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("생성된 회원의 ID, 실패 시 null").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    /**
     * 이미 등록된 이메일인 경우의 회원 가입 실패 테스트
     */
    @Test
    public void register_member_email_already_exists() throws Exception {
        // 요청 데이터 준비
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

        // 이미 등록된 이메일로 회원정보가 존재한다고 가정
        Student existingStudent = Student.builder()
                .name(form.getName())
                .email(form.getEmail())
                .password("encodedPassword")
                .nickname(form.getNickname())
                .studentId(form.getStudentId())
                .university(form.getUniversity())
                .gender(form.getGender())
                .phone(form.getPhone())
                .createdAt(LocalDate.now())
                .locked(false)
                .build();

        when(memberService.findByEmail("test@example.com")).thenReturn(Optional.of(existingStudent));

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("이미 등록된 이메일입니다."));
    }

    /**
     * 인증되지 않은 이메일의 경우의 회원 가입 실패 테스트
     */
    @Test
    public void register_member_email_not_certified() throws Exception {
        // 요청 데이터 준비
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setRole(0);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        // 이메일이 아직 인증되지 않은 상태로 가정
        when(memberService.findByEmail("test@ajou.ac.kr")).thenReturn(Optional.empty());
        when(univCertService.isCertified("test@ajou.ac.kr")).thenReturn(false);

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("미인증 이메일입니다."));
    }

    /**
     * [verify-email] 성공 케이스 테스트
     */
    @Test
    public void verify_email_success() throws Exception {
        RegisterVerifyRequestForm requestForm = new RegisterVerifyRequestForm();
        requestForm.setEmail("user@example.com");
        requestForm.setUniversity("Test University");

        when(univCertService.checkUniversity("Test University")).thenReturn(true);
        when(univCertService.certify("user@example.com", "Test University", false)).thenReturn(true);

        String json = objectMapper.writeValueAsString(requestForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                // 성공 시 success=true, data에 메시지, message 빈 문자열로 가정
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("이메일로 발송된 인증 코드를 확인하세요."))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("verify-email-success",
                        requestFields(
                                fieldWithPath("email").description("인증을 요청할 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("대학명").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("성공 시 반환되는 메시지").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * [verify-email] 실패 케이스 테스트 1 - 대학명이 유효하지 않은 경우
     */
    @Test
    public void verify_email_invalid_university() throws Exception {
        RegisterVerifyRequestForm requestForm = new RegisterVerifyRequestForm();
        requestForm.setEmail("user@example.com");
        requestForm.setUniversity("Invalid University");

        when(univCertService.checkUniversity("Invalid University")).thenReturn(false);

        String json = objectMapper.writeValueAsString(requestForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                // 실패 시 data가 null
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("유효하지 않은 대학명 또는 상위 150개 대학에 포함되지 않습니다."));
    }

    /**
     * [verify-email] 실패 케이스 테스트 2 - 인증 코드 발송 실패인 경우
     */
    @Test
    public void verify_email_certify_fails() throws Exception {
        RegisterVerifyRequestForm requestForm = new RegisterVerifyRequestForm();
        requestForm.setEmail("user@example.com");
        requestForm.setUniversity("Test University");

        when(univCertService.checkUniversity("Test University")).thenReturn(true);
        when(univCertService.certify("user@example.com", "Test University", false)).thenReturn(false);

        String json = objectMapper.writeValueAsString(requestForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                // 실패 시 data가 null
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("인증 코드 발송에 실패했습니다."));
    }

    /**
     * [verify-code] 성공 케이스 테스트
     * 대학명이 유효하고, 인증 코드가 올바른 경우 응답으로 true를 반환합니다.
     */
    @Test
    public void verify_code_success() throws Exception {
        RegisterVerifyCodeForm codeForm = new RegisterVerifyCodeForm();
        codeForm.setEmail("user@example.com");
        codeForm.setUniversity("Test University");
        codeForm.setCode(123456);

        when(univCertService.checkUniversity("Test University")).thenReturn(true);
        when(univCertService.certifyCode("user@example.com", "Test University", 123456))
                .thenReturn(true);

        String json = objectMapper.writeValueAsString(codeForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("verify-code-success",
                        requestFields(
                                fieldWithPath("email").description("인증 코드 검증 요청 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("대학명").type(JsonFieldType.STRING),
                                fieldWithPath("code").description("인증 코드").type(JsonFieldType.NUMBER)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("성공 시 true").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 오류 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * [verify-code] 실패 케이스 테스트 1 - 대학명이 유효하지 않은 경우
     */
    @Test
    public void verify_code_invalid_university() throws Exception {
        RegisterVerifyCodeForm codeForm = new RegisterVerifyCodeForm();
        codeForm.setEmail("user@example.com");
        codeForm.setUniversity("Invalid University");
        codeForm.setCode(123456);

        when(univCertService.checkUniversity("Invalid University")).thenReturn(false);

        String json = objectMapper.writeValueAsString(codeForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                // 실패 시 data가 null
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("유효하지 않은 대학명 또는 상위 150개 대학에 포함되지 않습니다."));
    }

    /**
     * [verify-code] 실패 케이스 테스트 2 - 인증 코드 검증 실패인 경우
     */
    @Test
    public void verifyCode_certifyCodeFails() throws Exception {
        RegisterVerifyCodeForm codeForm = new RegisterVerifyCodeForm();
        codeForm.setEmail("user@example.com");
        codeForm.setUniversity("Test University");
        codeForm.setCode(123456);

        when(univCertService.checkUniversity("Test University")).thenReturn(true);
        when(univCertService.certifyCode("user@example.com", "Test University", 123456))
                .thenReturn(false);

        String json = objectMapper.writeValueAsString(codeForm);

        mockMvc.perform(post("/api/v1/auth/signup/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                // 실패 시 data가 null
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("잘못된 인증 코드입니다."));
    }
}