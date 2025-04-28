package com.capstone.rentit.rental.controller;

import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.dto.RentalSearchForm;
import com.capstone.rentit.rental.service.RentalService;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentalController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class RentalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean RentalService rentalService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean MemberDetailsService memberDetailsService;

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals - 대여 요청 성공")
    @Test
    void requestRental_success() throws Exception {
        RentalRequestForm form = new RentalRequestForm();
        form.setItemId(100L);
        form.setOwnerId(10L);
        form.setRenterId(20L);
        form.setStartDate(LocalDateTime.now().plusDays(1));
        form.setDueDate(LocalDateTime.now().plusDays(7));

        given(rentalService.requestRental(any())).willReturn(1L);

        mockMvc.perform(post("/api/v1/rentals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1))
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

        verify(rentalService).requestRental(any());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/rentals - 내 대여 목록 조회")
    @Test
    void getMyRentals_success() throws Exception {
        //given
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        RentalDto dto = RentalDto.builder()
                .rentalId(1L).itemId(100L).ownerId(10L).renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now().plusDays(1))
                .dueDate(LocalDateTime.now().plusDays(7))
                .status(RentalStatusEnum.REQUESTED)
                .lockerId(null).paymentId(null).returnImageUrl(null)
                .approvedDate(null).rejectedDate(null).leftAt(null)
                .pickedUpAt(null).returnedAt(null).retrievedAt(null)
                .build();

        Page<RentalDto> dtoPage = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 20, Sort.by("requestDate").descending()),
                1
        );

        given(rentalService.getRentalsForUser(
                any(MemberDto.class),
                any(RentalSearchForm.class),
                any(Pageable.class))
        ).willReturn(dtoPage);

        //when, then
        mockMvc.perform(get("/api/v1/rentals")
                        .queryParam("statuses", "REQUESTED")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "requestDate,desc")
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rentalId").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andDo(document("get-my-rentals",
                        queryParameters(
                                parameterWithName("statuses")
                                        .description("조회할 대여 상태. 여러 개면 반복해서 전달 (예: ?statuses=REQUESTED&statuses=APPROVED)"),
                                parameterWithName("page")
                                        .description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size")
                                        .description("페이지 크기"),
                                parameterWithName("sort")
                                        .description("정렬 기준 (예: requestDate,desc)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.content[].rentalId").type(JsonFieldType.NUMBER).description("대여 정보 ID"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.content[].ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.content[].renterId").type(JsonFieldType.NUMBER).description("대여자 ID"),
                                fieldWithPath("data.content[].requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data.content[].approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data.content[].rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.content[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.content[].leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data.content[].pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data.content[].returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data.content[].retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data.content[].lockerId").type(JsonFieldType.NULL).description("사물함 ID"),
                                fieldWithPath("data.content[].paymentId").type(JsonFieldType.NULL).description("결제 ID"),
                                fieldWithPath("data.content[].returnImageUrl").type(JsonFieldType.NULL).description("반납 이미지 URL"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열"),

                                fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("현재 페이지의 요소 시작 인덱스"),
                                fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
                                fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("비페이징 여부"),
                                fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("페이지 정렬 정보 비어있는지 여부"),
                                fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("페이지 정렬 여부"),
                                fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("페이지 미정렬 여부"),

                                fieldWithPath("data.sort").type(JsonFieldType.OBJECT).description("전체 정렬 정보"),
                                fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어있는지 여부"),
                                fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 여부"),
                                fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("미정렬 여부"),

                                fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("요소 개수 (페이지 크기)"),
                                fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지의 실제 요소 수"),
                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("페이지가 비어있는지 여부")
                        )
                ));

        verify(rentalService).getRentalsForUser(
                any(MemberDto.class),
                any(RentalSearchForm.class),
                any(Pageable.class)
        );
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/rentals/{id} - 단일 대여 조회")
    @Test
    void getRental_success() throws Exception {
        long rid = 5L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        RentalDto dto = RentalDto.builder()
                .rentalId(rid).itemId(200L).ownerId(10L).renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now().plusDays(2))
                .dueDate(LocalDateTime.now().plusDays(8))
                .status(RentalStatusEnum.REQUESTED)
                .lockerId(null).paymentId(null).returnImageUrl(null)
                .approvedDate(null).rejectedDate(null).leftAt(null)
                .pickedUpAt(null).returnedAt(null).retrievedAt(null)
                .build();
        given(rentalService.getRental(eq(rid), any(MemberDto.class))).willReturn(dto);

        mockMvc.perform(get("/api/v1/rentals/{id}", rid)
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("get-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.rentalId").type(JsonFieldType.NUMBER).description("대여 정보 ID"),
                                fieldWithPath("data.itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.renterId").type(JsonFieldType.NUMBER).description("대여자 ID"),
                                fieldWithPath("data.requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data.approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data.rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data.pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data.returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data.retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data.lockerId").type(JsonFieldType.NULL).description("사물함 ID"),
                                fieldWithPath("data.paymentId").type(JsonFieldType.NULL).description("결제 ID"),
                                fieldWithPath("data.returnImageUrl").type(JsonFieldType.NULL).description("반납 이미지 URL"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));

        verify(rentalService).getRental(eq(rid), any());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/approve - 대여 승인")
    @Test
    void approveRental_success() throws Exception {
        long rid = 7L;
        mockMvc.perform(post("/api/v1/rentals/{id}/approve", rid)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("approve-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).approve(rid);
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/reject - 대여 거절")
    @Test
    void rejectRental_success() throws Exception {
        long rid = 8L;
        mockMvc.perform(post("/api/v1/rentals/{id}/reject", rid)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("reject-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).reject(rid);
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/cancel - 대여 취소")
    @Test
    void cancelRental_success() throws Exception {
        long rid = 7L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rid)
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("cancel-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).cancel(rid, student.getMemberId());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/dropoff - 사물함에 맡기기")
    @Test
    void dropOff_success() throws Exception {
        long rid = 9L, lockerId = 42L;
        Student student = Student.builder().memberId(10L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        mockMvc.perform(post("/api/v1/rentals/{id}/dropoff", rid)
                        .with(csrf())
                        .with(authentication(auth))
                        .queryParam("lockerId", String.valueOf(lockerId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("dropoff-rental",
                        RequestDocumentation.queryParameters(
                                parameterWithName("lockerId").attributes(key("type").value("number")).description("사물함 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).dropOffToLocker(rid, student.getMemberId(), lockerId);
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/pickup - 픽업")
    @Test
    void pickUpByRenter_success() throws Exception {
        long rid = 10L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        mockMvc.perform(post("/api/v1/rentals/{id}/pickup", rid)
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("pickup-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).pickUpByRenter(rid, student.getMemberId());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/return - 반납")
    @Test
    void returnToLocker_success() throws Exception {
        long rid = 13L, lockerId = 55L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        MockMultipartFile file = new MockMultipartFile("returnImage", "img.jpg", "image/jpeg", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/rentals/{id}/return", rid)
                        .file(file)
                        .with(csrf())
                        .with(authentication(auth))
                        .queryParam("lockerId", String.valueOf(lockerId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("return-rental",
                        requestParts(
                                partWithName("returnImage").attributes(key("type").value("file")).description("반납 이미지")
                        ),
                        RequestDocumentation.queryParameters(
                                parameterWithName("lockerId").attributes(key("type").value("number")).description("사물함 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).returnToLocker(eq(rid), eq(student.getMemberId()), eq(lockerId), any());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{id}/retrieve - 회수 완료")
    @Test
    void retrieveByOwner_success() throws Exception {
        long rid = 15L;
        Student student = Student.builder().memberId(10L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        mockMvc.perform(post("/api/v1/rentals/{id}/retrieve", rid)
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("retrieve-rental",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지")
                        )
                ));

        verify(rentalService).retrieveByOwner(rid, student.getMemberId());
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/rentals/{userId} - 관리자 특정 사용자 대여 목록")
    @Test
    void getRentalsByUser_success() throws Exception {
        long userId = 99L;
        RentalDto dto1 = RentalDto.builder()
                .rentalId(20L).itemId(100L).ownerId(10L).renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(1))
                .status(RentalStatusEnum.REQUESTED)
                .lockerId(null).paymentId(null).returnImageUrl(null)
                .approvedDate(null).rejectedDate(null).leftAt(null)
                .pickedUpAt(null).returnedAt(null).retrievedAt(null)
                .build();
        RentalDto dto2 = RentalDto.builder()
                .rentalId(21L).itemId(101L).ownerId(10L).renterId(21L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(1))
                .status(RentalStatusEnum.REQUESTED)
                .lockerId(null).paymentId(null).returnImageUrl(null)
                .approvedDate(null).rejectedDate(null).leftAt(null)
                .pickedUpAt(null).returnedAt(null).retrievedAt(null)
                .build();
        given(rentalService.getRentalsByUser(userId)).willReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/admin/rentals/{userId}", userId)
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("get-rentals-by-user",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data[].rentalId").type(JsonFieldType.NUMBER).description("대여 정보 ID"),
                                fieldWithPath("data[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data[].ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data[].renterId").type(JsonFieldType.NUMBER).description("대여자 ID"),
                                fieldWithPath("data[].requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data[].approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data[].rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data[].leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data[].pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data[].returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data[].retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data[].lockerId").type(JsonFieldType.NULL).description("사물함 ID"),
                                fieldWithPath("data[].paymentId").type(JsonFieldType.NULL).description("결제 ID"),
                                fieldWithPath("data[].returnImageUrl").type(JsonFieldType.NULL).description("반납 이미지 URL"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));

        verify(rentalService).getRentalsByUser(userId);
    }
}
