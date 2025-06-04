package com.capstone.rentit.rental.controller;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.dto.*;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
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
    @MockitoBean FileStorageService fileStorageService;

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals - 대여 요청 성공")
    @Test
    void requestRental_success() throws Exception {
        // given
        RentalRequestForm form = new RentalRequestForm();
        form.setItemId(100L);
        form.setOwnerId(10L);
        form.setRenterId(20L);
        form.setStartDate(LocalDateTime.now().plusDays(1));
        form.setDueDate(LocalDateTime.now().plusDays(7));

        given(rentalService.requestRental(any())).willReturn(1L);

        // when, then
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
        // given
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        RentalDto dto = RentalDto.builder()
                .rentalId(1L).itemId(100L).ownerId(10L).renterId(20L)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now().plusDays(1))
                .dueDate(LocalDateTime.now().plusDays(7))
                .status(RentalStatusEnum.REQUESTED)
                .lockerId(null).returnImageUrl(null)
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

        // when, then
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
                        relaxedResponseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),

                                fieldWithPath("data.content[].rentalId").type(JsonFieldType.NUMBER).description("대여 정보 ID"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.content[].ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.content[].renterId").type(JsonFieldType.NUMBER).description("대여자 ID"),
                                fieldWithPath("data.content[].requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data.content[].approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data.content[].rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.content[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.content[].leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data.content[].pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data.content[].returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data.content[].retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.content[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.content[].lockerId").type(JsonFieldType.NULL).description("사물함 ID (없으면 null)"),
                                fieldWithPath("data.content[].returnImageUrl").type(JsonFieldType.NULL).description("반납 이미지 (없으면 null)"),

                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기")
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
        // given
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
                .lockerId(null).returnImageUrl(null)
                .approvedDate(null).rejectedDate(null).leftAt(null)
                .pickedUpAt(null).returnedAt(null).retrievedAt(null)
                .build();
        given(rentalService.getRental(eq(rid), any(Long.class))).willReturn(dto);

        // when, then
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
                                fieldWithPath("data.approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data.rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data.pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data.returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data.retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.lockerId").type(JsonFieldType.NULL).description("사물함 ID"),
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
        // given
        long rid = 7L;

        // when, then
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
        // given
        long rid = 8L;

        // when, then
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
        // given
        long rid = 7L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        // when, then
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

    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/rentals/{userId} - 관리자 특정 사용자 대여 목록")
    @Test
    void getRentalsByUser_success() throws Exception {
        // given
        long userId = 99L;
        RentalBriefResponse brief1 = RentalBriefResponse.builder()
                .rentalId(20L)
                .itemId(100L)
                .itemName("ItemA")
                .ownerName("OwnerA")
                .renterName("RenterA")
                .status(RentalStatusEnum.REQUESTED)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(1))
                .thumbnailUrl("https://example.com/thumbA")
                .lockerUniversity("UnivA")
                .lockerLocation("LocA")
                .lockerNumber(1L)
                .isOwner(true)
                .build();

        RentalBriefResponse brief2 = RentalBriefResponse.builder()
                .rentalId(21L)
                .itemId(101L)
                .itemName("ItemB")
                .ownerName("OwnerB")
                .renterName("RenterB")
                .status(RentalStatusEnum.REQUESTED)
                .requestDate(LocalDateTime.now())
                .startDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(1))
                .thumbnailUrl("https://example.com/thumbB")
                .lockerUniversity("UnivB")
                .lockerLocation("LocB")
                .lockerNumber(2L)
                .isOwner(false)
                .build();

        given(rentalService.getRentalsByUser(userId))
                .willReturn(List.of(brief1, brief2));

        // when, then
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
                                fieldWithPath("data[].itemName").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("data[].ownerName").type(JsonFieldType.STRING).description("소유자 이름"),
                                fieldWithPath("data[].renterName").type(JsonFieldType.STRING).description("대여자 이름"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data[].requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data[].approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data[].rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data[].leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data[].pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data[].returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data[].retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data[].thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 URL"),
                                fieldWithPath("data[].lockerUniversity").type(JsonFieldType.STRING).description("사물함 대학"),
                                fieldWithPath("data[].lockerLocation").type(JsonFieldType.STRING).description("사물함 위치"),
                                fieldWithPath("data[].lockerNumber").type(JsonFieldType.NUMBER).description("사물함 번호"),
                                fieldWithPath("data[].owner").type(JsonFieldType.BOOLEAN).description("내가 소유자인지 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));

        verify(rentalService).getRentalsByUser(userId);
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/rentals/{rentalId}/return-image — 반납 이미지 키 업로드 성공")
    @Test
    void uploadReturnImage_success() throws Exception {
        // given
        long rentalId = 42L;
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        String returnImageKey = "stored/object/key.jpg";

        willDoNothing().given(rentalService)
                .uploadReturnImage(eq(rentalId), eq(student.getMemberId()), eq(returnImageKey));

        // when & then
        mockMvc.perform(post("/api/v1/rentals/{rentalId}/return-image", rentalId)
                        .queryParam("returnImageKey", returnImageKey)
                        .with(csrf())
                        .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").isEmpty())
                .andDo(document("upload-return-image",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("rentalId").description("대여 정보 ID")
                        ),
                        queryParameters(
                                parameterWithName("returnImageKey").description("반납 이미지 object key")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 (항상 null)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열, 실패 시 에러 메시지")
                        )
                ));

        verify(rentalService).uploadReturnImage(eq(rentalId), eq(student.getMemberId()), eq(returnImageKey));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/rentals - 관리자 전체 대여 목록 조회")
    @Test
    void getAllRentals_success() throws Exception {
        // given
        RentalBriefResponse brief1 = RentalBriefResponse.builder()
                .rentalId(30L)
                .itemId(200L)
                .itemName("ItemX")
                .ownerName("OwnerX")
                .renterName("RenterX")
                .status(RentalStatusEnum.APPROVED)
                .requestDate(LocalDateTime.now().minusDays(5))
                .startDate(LocalDateTime.now().minusDays(3))
                .dueDate(LocalDateTime.now().plusDays(2))
                .thumbnailUrl("https://example.com/thumbX")
                .lockerUniversity("UnivX")
                .lockerLocation("LocX")
                .lockerNumber(5L)
                .isOwner(false)
                .build();

        RentalBriefResponse brief2 = RentalBriefResponse.builder()
                .rentalId(31L)
                .itemId(201L)
                .itemName("ItemY")
                .ownerName("OwnerY")
                .renterName("RenterY")
                .status(RentalStatusEnum.REQUESTED)
                .requestDate(LocalDateTime.now().minusDays(2))
                .startDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(5))
                .thumbnailUrl("https://example.com/thumbY")
                .lockerUniversity("UnivY")
                .lockerLocation("LocY")
                .lockerNumber(6L)
                .isOwner(true)
                .build();

        Page<RentalBriefResponse> briefPage = new PageImpl<>(
                List.of(brief1, brief2),
                PageRequest.of(0, 2, Sort.by("requestDate").descending()),
                2
        );

        given(rentalService.getAllRentals(
                any(RentalSearchForm.class),
                any(Pageable.class))
        ).willReturn(briefPage);

        // when, then
        mockMvc.perform(get("/api/v1/admin/rentals")
                        .queryParam("statuses", "APPROVED")
                        .queryParam("statuses", "REQUESTED")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("sort", "requestDate,desc")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rentalId").value(30))
                .andExpect(jsonPath("$.data.content[1].rentalId").value(31))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andDo(document("get-all-rentals",
                        queryParameters(
                                parameterWithName("statuses")
                                        .description("조회할 대여 상태 목록 (반복 가능)"),
                                parameterWithName("page")
                                        .description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size")
                                        .description("페이지 크기"),
                                parameterWithName("sort")
                                        .description("정렬 기준 (예: requestDate,desc)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),

                                // content 내 주요 필드
                                fieldWithPath("data.content[].rentalId").type(JsonFieldType.NUMBER).description("대여 정보 ID"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.content[].itemName").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("data.content[].ownerName").type(JsonFieldType.STRING).description("소유자 이름"),
                                fieldWithPath("data.content[].renterName").type(JsonFieldType.STRING).description("대여자 이름"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("대여 상태"),
                                fieldWithPath("data.content[].requestDate").type(JsonFieldType.STRING).description("요청 일시"),
                                fieldWithPath("data.content[].approvedDate").type(JsonFieldType.NULL).description("승인 일시 (없으면 null)"),
                                fieldWithPath("data.content[].rejectedDate").type(JsonFieldType.NULL).description("거절 일시 (없으면 null)"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.content[].dueDate").type(JsonFieldType.STRING).description("반납 예정일"),
                                fieldWithPath("data.content[].leftAt").type(JsonFieldType.NULL).description("사물함에 맡긴 시각 (없으면 null)"),
                                fieldWithPath("data.content[].pickedUpAt").type(JsonFieldType.NULL).description("픽업 시각 (없으면 null)"),
                                fieldWithPath("data.content[].returnedAt").type(JsonFieldType.NULL).description("반납 시각 (없으면 null)"),
                                fieldWithPath("data.content[].retrievedAt").type(JsonFieldType.NULL).description("회수 시각 (없으면 null)"),
                                fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 URL"),
                                fieldWithPath("data.content[].lockerUniversity").type(JsonFieldType.STRING).description("사물함 대학"),
                                fieldWithPath("data.content[].lockerLocation").type(JsonFieldType.STRING).description("사물함 위치"),
                                fieldWithPath("data.content[].lockerNumber").type(JsonFieldType.NUMBER).description("사물함 번호"),
                                fieldWithPath("data.content[].owner").type(JsonFieldType.BOOLEAN).description("내가 소유자인지 여부"),

                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),

                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));

        verify(rentalService).getAllRentals(
                any(RentalSearchForm.class),
                any(Pageable.class)
        );
    }
}