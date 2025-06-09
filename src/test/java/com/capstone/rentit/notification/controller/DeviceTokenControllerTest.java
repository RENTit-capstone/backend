package com.capstone.rentit.notification.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.notification.dto.TokenRequest;
import com.capstone.rentit.notification.service.DeviceTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceTokenController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class DeviceTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    MemberDetailsService memberDetailsService;
    @MockitoBean
    FileStorageService fileStorageService;

    @Test
    @DisplayName("POST /api/v1/device-token – 정상 호출 시 서비스가 토큰을 저장한다")
    void registerToken_success() throws Exception {
        //given
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        TokenRequest req = new TokenRequest("token-abc");
        String json = objectMapper.writeValueAsString(req);

        // when / then
        mockMvc.perform(post("/api/v1/device-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andDo(document("device-token-register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("token").description("FCM 디바이스 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data").description("반환 데이터 (항상 null)"),
                                fieldWithPath("message").description("실패 시 에러 메시지")
                        )
                ));

        verify(tokenService, times(1)).saveToken(any(Long.class), any(String.class));
    }
}
