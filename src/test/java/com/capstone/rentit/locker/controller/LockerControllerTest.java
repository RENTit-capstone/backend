package com.capstone.rentit.locker.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.locker.dto.LockerCreateForm;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LockerController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ExtendWith(SpringExtension.class)
class LockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LockerService lockerService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        // JWT 필터가 실제로 인증을 방해하지 않도록 통과시켜 줍니다.
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/admin/lockers → 생성 후 ID 반환")
    @Test
    void registerLocker_success() throws Exception {
        // given
        LockerCreateForm form = LockerCreateForm.builder()
                .university("Seoul Univ").locationDescription("building 1, floor 3").build();
        long generatedId = 5L;
        given(lockerService.registerLocker(any(LockerCreateForm.class)))
                .willReturn(generatedId);

        // when / then
        mockMvc.perform(post("/api/v1/admin/lockers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(generatedId))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("admin-register-locker",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("university").type(JsonFieldType.STRING).description("학교 이름"),
                                fieldWithPath("locationDescription").type(JsonFieldType.STRING).description("사물함 위치 상세 설명")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 사물함 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/lockers → 검색 파라미터에 따른 목록 반환")
    @Test
    void listLockers_success() throws Exception {
        // given
        LockerDto a = LockerDto.builder()
                .lockerId(1L).available(true).university("U1")
                .locationDescription("location1").activatedAt(LocalDateTime.now())
                .build();
        LockerDto b = LockerDto.builder()
                .lockerId(2L).available(false).university("U2")
                .locationDescription("location2").activatedAt(LocalDateTime.now())
                .build();
        given(lockerService.searchLockers(any(LockerSearchForm.class)))
                .willReturn(List.of(a, b));

        // when / then
        mockMvc.perform(get("/api/v1/admin/lockers")
                        .queryParam("university", "U")
                        .queryParam("available", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].lockerId").value(1))
                .andExpect(jsonPath("$.data[0].university").value("U1"))
                .andExpect(jsonPath("$.data[0].available").value(true))
                .andExpect(jsonPath("$.data[1].lockerId").value(2))
                .andExpect(jsonPath("$.data[1].university").value("U2"))
                .andExpect(jsonPath("$.data[1].available").value(false))
                .andDo(document("admin-list-lockers",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("university").optional().description("검색할 학교 이름"),
                                parameterWithName("available").optional().description("사용 가능 여부")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                fieldWithPath("data[].lockerId").type(JsonFieldType.NUMBER).description("사물함 ID"),
                                fieldWithPath("data[].university").type(JsonFieldType.STRING).description("학교 이름"),
                                fieldWithPath("data[].locationDescription").type(JsonFieldType.STRING).description("사물함 위치 상세 설명"),
                                fieldWithPath("data[].available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부"),
                                fieldWithPath("data[].activatedAt").type(JsonFieldType.STRING).description("사물함 활성화 시간"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/lockers/{lockerId} → 단일 사물함 조회")
    @Test
    void getLocker_success() throws Exception {
        // given
        LockerDto dto = LockerDto.builder()
                .lockerId(3L).available(true).university("U3")
                .locationDescription("location3").activatedAt(LocalDateTime.now())
                .build();
        given(lockerService.getLocker(3L)).willReturn(dto);

        // when / then
        mockMvc.perform(get("/api/v1/admin/lockers/{lockerId}", 3L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lockerId").value(3))
                .andExpect(jsonPath("$.data.university").value("U3"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("admin-get-locker",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("lockerId").description("사물함 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                fieldWithPath("data.lockerId").type(JsonFieldType.NUMBER).description("사물함 ID"),
                                fieldWithPath("data.university").type(JsonFieldType.STRING).description("학교 이름"),
                                fieldWithPath("data.locationDescription").type(JsonFieldType.STRING).description("사물함 위치 상세 설명"),
                                fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부"),
                                fieldWithPath("data.activatedAt").type(JsonFieldType.STRING).description("사물함 활성화 시간"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )));
    }
}