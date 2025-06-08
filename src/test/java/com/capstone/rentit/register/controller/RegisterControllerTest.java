package com.capstone.rentit.register.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.register.dto.RegisterVerifyCodeForm;
import com.capstone.rentit.register.dto.RegisterVerifyRequestForm;
import com.capstone.rentit.register.service.UnivCertService;
import com.capstone.rentit.register.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegisterController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private VerificationService verificationService;
    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @DisplayName("회원 가입 성공")
    @Test
    void register_member_success() throws Exception {
        // given
        StudentCreateForm form = new StudentCreateForm();
        form.setEmail("new@example.com");
        form.setPassword("pass");
        form.setName("Tester");
        form.setNickname("nick");
        form.setUniversity("Uni");
        form.setStudentId("S123");
        form.setGender(GenderEnum.WOMEN);
        form.setPhone("010");
        form.setProfileImg("img");

        doNothing().when(memberService).ensureEmailNotRegistered("new@example.com");
        doNothing().when(verificationService).ensureEmailVerified(form.getEmail());
        when(memberService.createMember(any())).thenReturn(42L);

        // when
        var result = mockMvc.perform(post("/api/v1/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("register-member-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("사용자 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("memberType").optional().type(JsonFieldType.STRING).description("사용자 타입(STUDENT, COUNCIL, COMPANY)"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("사용자 소속 대학"),
                                fieldWithPath("studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("gender").type(JsonFieldType.STRING).description("성별(MEM, WOMEN)"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).description("연락처"),
                                fieldWithPath("profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 (선택)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 회원의 ID, 실패 시 null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @DisplayName("회사 가입 성공")
    @Test
    void register_company_member_success() throws Exception {
        // given
        CompanyCreateForm form = new CompanyCreateForm();
        form.setEmail("new_company@example.com");
        form.setPassword("password");
        form.setName("test company");
        form.setDescription("company description");
        form.setCompanyName("company name");
        form.setIndustry("IT");
        form.setContactEmail("new_company@contact.com");
        form.setRegistrationNumber("000010100");
        form.setProfileImg("img");

        when(memberService.createMember(any())).thenReturn(62L);

        // when
        var result = mockMvc.perform(post("/api/v1/auth/signup/group")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(62L))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("register-company-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("회사 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("회사 비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("타인에게 보일 회사 이름"),
                                fieldWithPath("memberType").optional().type(JsonFieldType.STRING).description("사용자 타입(COMPANY)"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("회사 설명"),
                                fieldWithPath("companyName").type(JsonFieldType.STRING).description("회사 이름"),
                                fieldWithPath("industry").type(JsonFieldType.STRING).description("회사 분야"),
                                fieldWithPath("contactEmail").type(JsonFieldType.STRING).description("회사 연락처"),
                                fieldWithPath("registrationNumber").type(JsonFieldType.STRING).description("회사 사업자 번호"),
                                fieldWithPath("profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 (선택)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 회원의 ID, 실패 시 null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @DisplayName("학생회 가입 성공")
    @Test
    void register_council_success() throws Exception {
        // given
        StudentCouncilMemberCreateForm form = new StudentCouncilMemberCreateForm();
        form.setEmail("new_council@example.com");
        form.setPassword("password");
        form.setName("council name");
        form.setUniversity("university_name");
        form.setDescription("council description");
        form.setProfileImg("img");

        when(memberService.createMember(any())).thenReturn(72L);

        // when
        var result = mockMvc.perform(post("/api/v1/auth/signup/group")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(72L))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("register-council-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("학생회 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("학생회 비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("학생회 이름"),
                                fieldWithPath("memberType").optional().type(JsonFieldType.STRING).description("사용자 타입(COUNCIL)"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("학생회 소속 대학"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("학생회 설명"),
                                fieldWithPath("profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 (선택)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 회원의 ID, 실패 시 null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @DisplayName("verify-email 성공")
    @Test
    void verify_email_success() throws Exception {
        // given
        RegisterVerifyRequestForm form = new RegisterVerifyRequestForm();
        form.setEmail("a@b.com");
        form.setUniversity("Uni");

        doNothing().when(verificationService).generateAndSendVerificationCode(form.getEmail());

        // when
        var result = mockMvc.perform(post("/api/v1/auth/signup/verify-email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("이메일로 발송된 인증 코드를 확인하세요."))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("verify-email-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("인증을 요청할 이메일"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("대학명")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.STRING).description("발송 안내 메시지"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @DisplayName("verify-code 성공")
    @Test
    void verify_code_success() throws Exception {
        // given
        RegisterVerifyCodeForm form = new RegisterVerifyCodeForm();
        form.setEmail("x@y.com");
        form.setUniversity("Uni");
        form.setCode(123);

        doNothing().when(verificationService).verifyCode(form.getEmail(), form.getCode());

        // when
        var result = mockMvc.perform(post("/api/v1/auth/signup/verify-code")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("verify-code-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("인증 요청 이메일"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("대학명"),
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("인증 코드")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("성공 시 true"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

}