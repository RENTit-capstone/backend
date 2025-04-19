package com.capstone.rentit.rental.controller;

import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.service.RentalService;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.apache.logging.log4j.util.Lazy.value;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class RentalControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    RentalService rentalService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals - 대여 요청 성공")
    void requestRental_success() throws Exception {
        RentalRequestForm form = new RentalRequestForm();
        form.setItemId(100L);
        form.setOwnerId(10L);
        form.setRenterId(20L);
        form.setStartDate(LocalDateTime.now().plusDays(1));
        form.setDueDate(LocalDateTime.now().plusDays(7));

        long rentalId = 1L;
        when(rentalService.requestRental(any(RentalRequestForm.class))).thenReturn(rentalId);

        mockMvc.perform(post("/api/v1/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(rentalId))
                .andDo(document("request-rental",
                        requestFields(
                                fieldWithPath("itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("renterId").type(JsonFieldType.NUMBER).description("대여자 ID"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("예정 대여일"),
                                fieldWithPath("dueDate").type(JsonFieldType.STRING).description("반납 예정일")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 대여 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/rentals - 내 대여 목록 조회")
    void getMyRentals_success() throws Exception {
        // 로그인된 사용자
        Student student = Student.builder().memberId(20L).email("user@test.com").name("User").role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(student);

        RentalDto dto = RentalDto.builder()
                .rentalId(1L)
                .itemId(100L)
                .ownerId(10L)
                .renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now().plusDays(1))
                .status(RentalStatusEnum.REQUESTED)
                .dueDate(LocalDateTime.now().plusDays(7))
                .lockerId(null)
                .paymentId(null)
                .build();
        when(rentalService.getRentalsForUser(any(MemberDto.class)))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/rentals")
                        .with(user(details))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rentalId").value(1))
                .andDo(document("get-my-rentals",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data[].rentalId").description("대여 정보 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].itemId").description("대여 물품 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].ownerId").description("물품 소유자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].renterId").description("물품 대여자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].requestDate").description("대여 요청 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].status").description("대여 상태").type(JsonFieldType.STRING),
                                fieldWithPath("data[].approvedDate").description("승인 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].rejectedDate").description("거절 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].startDate").description("예정 대여 시작 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].dueDate").description("대여 만료 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].leftAt").description("소유자 물건 맡긴 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].pickedUpAt").description("대여자 픽업 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].returnedAt").description("대여자 반납 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].retrievedAt").description("소유자 회수 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].lockerId").description("사물함 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].paymentId").description("결제 정보 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("message").description("성공 시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/rentals/{rentalId} - 단일 대여 조회")
    void getRental_success() throws Exception {
        Student student = Student.builder().memberId(20L).email("user@test.com").name("User").role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(student);

        RentalDto dto = RentalDto.builder()
                .rentalId(5L)
                .itemId(200L)
                .ownerId(10L)
                .renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now().plusDays(2))
                .status(RentalStatusEnum.REQUESTED)
                .dueDate(LocalDateTime.now().plusDays(8))
                .lockerId(null)
                .paymentId(null)
                .build();

        when(rentalService.getRental(eq(5L), any(MemberDto.class))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/rentals/5")
                        .with(user(details))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rentalId").value(5))
                .andDo(document("get-rental",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.rentalId").description("대여 정보 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.itemId").description("대여 물품 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.ownerId").description("물품 소유자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.renterId").description("물품 대여자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.requestDate").description("대여 요청 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data.status").description("대여 상태").type(JsonFieldType.STRING),
                                fieldWithPath("data.approvedDate").description("승인 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.rejectedDate").description("거절 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.startDate").description("예정 대여 시작 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data.dueDate").description("대여 만료 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data.leftAt").description("소유자 물건 맡긴 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.pickedUpAt").description("대여자 픽업 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.returnedAt").description("대여자 반납 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.retrievedAt").description("소유자 회수 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.lockerId").description("사물함 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("data.paymentId").description("결제 정보 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("message").description("성공 시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/approve - 대여 승인")
    void approveRental_success() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/5/approve")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("approve-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(value(nullValue())).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/reject - 대여 거절")
    void rejectRental_success() throws Exception {
        mockMvc.perform(post("/api/v1/rentals/5/reject")
                        .with(user("owner").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("reject-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(value(nullValue())).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).reject(5L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/cancel - 대여 취소")
    void cancelRental_success() throws Exception {
        // 로그인된 대여자 정보
        Student renter = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(renter);

        mockMvc.perform(post("/api/v1/rentals/7/cancel")
                        .with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("cancel-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(value(nullValue())).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).cancel(7L, 20L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/dropoff - 소유자 사물함에 맡기기")
    void dropOff_success() throws Exception {
        Student owner = Student.builder().memberId(10L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(owner);

        mockMvc.perform(post("/api/v1/rentals/9/dropoff")
                        .with(user(details))
                        .queryParam("lockerId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("dropoff-rental",
                        queryParameters(
                                parameterWithName("lockerId").description("사물함 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).dropOffToLocker(9L, 10L, 42L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/pickup - 대여자가 픽업")
    void pickUpByRenter_success() throws Exception {
        Student renter = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(renter);

        mockMvc.perform(post("/api/v1/rentals/11/pickup")
                        .with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("pickup-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(value(nullValue())).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).pickUpByRenter(11L, 20L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/return - 대여자가 반납")
    void returnToLocker_success() throws Exception {
        Student renter = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(renter);

        MockMultipartFile returnImage = new MockMultipartFile(
                "returnImage",             // @RequestPart name
                "return.jpg",              // original filename
                "image/jpeg",              // content type
                "dummy-image-bytes".getBytes() // content
        );

        mockMvc.perform(multipart("/api/v1/rentals/13/return")
                        .file(returnImage)
                        .with(user(details))
                        .queryParam("lockerId", "55")
                            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("return-rental",
                        requestParts(
                                partWithName("returnImage").description("반납 시 찍은 물품 이미지")
                        ),
                        queryParameters(
                                parameterWithName("lockerId").description("사물함 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).returnToLocker(
                eq(13L),
                eq(20L),
                eq(55L),
                any(MultipartFile.class)
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/retrieve - 소유자가 회수 (완료)")
    void retrieveByOwner_success() throws Exception {
        Student owner = Student.builder().memberId(10L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails details = new MemberDetails(owner);

        mockMvc.perform(post("/api/v1/rentals/15/retrieve")
                        .with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("retrieve-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(value(nullValue())).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).retrieveByOwner(15L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/rentals/{userId} - 관리자 특정 사용자 대여 목록")
    void getRentalsByUser_success() throws Exception {
        RentalDto dto1 = RentalDto.builder()
                .rentalId(20L)
                .itemId(100L)
                .ownerId(10L)
                .renterId(20L)
                .requestDate(LocalDateTime.now())
                .status(RentalStatusEnum.REQUESTED)
                .startDate(LocalDateTime.now().plusDays(2))
                .dueDate(LocalDateTime.now().plusDays(8))
                .paymentId(55L)
                .build();
        RentalDto dto2 = RentalDto.builder()
                .rentalId(21L)
                .itemId(100L)
                .ownerId(10L)
                .renterId(21L)
                .requestDate(LocalDateTime.now())
                .status(RentalStatusEnum.REQUESTED)
                .startDate(LocalDateTime.now().plusDays(2))
                .dueDate(LocalDateTime.now().plusDays(8))
                .paymentId(55L)
                .build();
        when(rentalService.getRentalsByUser(99L)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/admin/rentals/99")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rentalId").value(20))
                .andDo(document("get-rentals-by-user",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data[].rentalId").description("대여 정보 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].itemId").description("대여 물품 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].ownerId").description("물품 소유자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].renterId").description("물품 대여자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].requestDate").description("대여 요청 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].status").description("대여 상태").type(JsonFieldType.STRING),
                                fieldWithPath("data[].approvedDate").description("승인 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].rejectedDate").description("거절 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].startDate").description("예정 대여 시작 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].dueDate").description("대여 만료 일시").type(JsonFieldType.STRING),
                                fieldWithPath("data[].leftAt").description("소유자 물건 맡긴 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].pickedUpAt").description("대여자 픽업 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].returnedAt").description("대여자 반납 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].retrievedAt").description("소유자 회수 일시").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].lockerId").description("사물함 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].paymentId").description("결제 정보 ID").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("message").description("성공 시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));

        verify(rentalService).getRentalsByUser(99L);
    }
}