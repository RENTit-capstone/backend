package com.capstone.rentit.otp.controller;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.otp.exception.OtpMismatchException;
import com.capstone.rentit.otp.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtpController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OtpService otpService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    MemberDetailsService memberDetailsService;
    @MockitoBean
    private FileStorageService fileStorageService;

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("POST /api/v1/auth/otp - 생성된 OTP 코드 반환")
    void requestOtp_success() throws Exception {
        //given
        Student student = Student.builder()
                .memberId(99L)
                .email("otp@student.com")
                .role(MemberRoleEnum.STUDENT)
                .build();
        MemberDetails details = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        given(otpService.generateOtp(anyString())).willReturn("12345");

        //when, then
        mockMvc.perform(post("/api/v1/auth/otp")
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("12345"))
                .andDo(document("otp-request",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.STRING).description("생성된 OTP 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열 또는 에러 메시지")
                        )
                ));
    }
}