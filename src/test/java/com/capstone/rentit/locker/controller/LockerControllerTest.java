package com.capstone.rentit.locker.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("POST /api/v1/admin/devices")
    class RegisterDevice {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("정상 등록 시 생성된 deviceId 반환 및 문서화")
        void registerDevice_success() throws Exception {
            DeviceCreateForm form = new DeviceCreateForm("Univ1", "Location1");
            long generatedId = 123L;
            given(lockerService.registerDevice(any(DeviceCreateForm.class)))
                    .willReturn(generatedId);

            mockMvc.perform(post("/api/v1/admin/devices")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(generatedId))
                    .andExpect(jsonPath("$.message").value(""))
                    .andDo(document("admin-register-device",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("university").type(JsonFieldType.STRING).description("학교 이름"),
                                    fieldWithPath("locationDescription").type(JsonFieldType.STRING).description("사물함 위치 상세 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                    fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 deviceId"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/devices")
    class ListDevices {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("대학명으로 Device 목록 조회 및 문서화")
        void listDevices_success() throws Exception {
            DeviceResponse a = DeviceResponse.builder()
                    .deviceId(1L)
                    .university("U1")
                    .locationDescription("Loc1")
                    .build();
            DeviceResponse b = DeviceResponse.builder()
                    .deviceId(2L)
                    .university("U1")
                    .locationDescription("Loc2")
                    .build();
            given(lockerService.searchDevicesByUniversity(any(DeviceSearchForm.class)))
                    .willReturn(List.of(a, b));

            mockMvc.perform(get("/api/v1/admin/devices")
                            .param("university", "U1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].deviceId").value(1))
                    .andExpect(jsonPath("$.data[0].university").value("U1"))
                    .andExpect(jsonPath("$.data[0].locationDescription").value("Loc1"))
                    .andExpect(jsonPath("$.data[1].deviceId").value(2))
                    .andExpect(jsonPath("$.data[1].locationDescription").value("Loc2"))
                    .andDo(document("admin-list-devices",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("university").description("검색할 학교 이름")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                    fieldWithPath("data[].deviceId").type(JsonFieldType.NUMBER).description("디바이스 ID"),
                                    fieldWithPath("data[].university").type(JsonFieldType.STRING).description("학교 이름"),
                                    fieldWithPath("data[].locationDescription").type(JsonFieldType.STRING).description("사물함 위치 상세 설명"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/lockers")
    class RegisterLocker {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("정상 등록 시 생성된 lockerId 반환 및 문서화")
        void registerLocker_success() throws Exception {
            LockerCreateForm form = new LockerCreateForm(5L);
            long generatedId = 321L;
            given(lockerService.registerLocker(any(LockerCreateForm.class)))
                    .willReturn(generatedId);

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
                                    fieldWithPath("deviceId").type(JsonFieldType.NUMBER).description("키오스크 디바이스 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                    fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 칸 ID"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/lockers")
    class ListLockers {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("deviceId 및 available로 Locker 목록 조회 및 문서화")
        void listLockers_success() throws Exception {
            Device device = Device.builder()
                    .deviceId(10L)
                    .university("U2")
                    .locationDescription("DescX")
                    .build();
            LockerResponse x = LockerResponse.builder()
                    .deviceId(10L)
                    .lockerId(1L)
                    .available(true)
                    .activatedAt(LocalDateTime.now())
                    .device(DeviceResponse.fromEntity(device))
                    .build();
            Device device2 = Device.builder()
                    .deviceId(10L)
                    .university("U2")
                    .locationDescription("DescY")
                    .build();
            LockerResponse y = LockerResponse.builder()
                    .deviceId(10L)
                    .lockerId(2L)
                    .available(false)
                    .activatedAt(LocalDateTime.now())
                    .device(DeviceResponse.fromEntity(device2))
                    .build();
            given(lockerService.searchLockers(any(LockerSearchForm.class)))
                    .willReturn(List.of(x, y));

            mockMvc.perform(get("/api/v1/admin/lockers")
                            .param("deviceId", "10")
                            .param("available", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].deviceId").value(10))
                    .andExpect(jsonPath("$.data[0].lockerId").value(1))
                    .andExpect(jsonPath("$.data[0].available").value(true))
                    .andExpect(jsonPath("$.data[0].device.deviceId").value(10))
                    .andExpect(jsonPath("$.data[0].device.university").value("U2"))
                    .andExpect(jsonPath("$.data[0].device.locationDescription").value("DescX"))
                    .andExpect(jsonPath("$.data[1].lockerId").value(2))
                    .andDo(document("admin-list-lockers",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("deviceId").description("키오스크 디바이스 ID"),
                                    parameterWithName("available").description("사용 가능 여부")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 성공 여부"),
                                    fieldWithPath("data[].deviceId").type(JsonFieldType.NUMBER).description("사물함 디바이스 ID"),
                                    fieldWithPath("data[].lockerId").type(JsonFieldType.NUMBER).description("칸 ID"),
                                    fieldWithPath("data[].available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부"),
                                    fieldWithPath("data[].activatedAt").type(JsonFieldType.STRING).description("활성화 시간"),
                                    fieldWithPath("data[].device.deviceId").type(JsonFieldType.NUMBER).description("디바이스 ID"),
                                    fieldWithPath("data[].device").type(JsonFieldType.OBJECT).description("디바이스 상세 정보"),
                                    fieldWithPath("data[].device.university").type(JsonFieldType.STRING).description("학교 이름"),
                                    fieldWithPath("data[].device.locationDescription").type(JsonFieldType.STRING).description("칸 위치 상세 설명"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                            )
                    ));
        }
    }
}
