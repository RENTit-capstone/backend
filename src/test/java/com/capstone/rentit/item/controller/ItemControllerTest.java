package com.capstone.rentit.item.controller;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.dto.MemberSearchResponse;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.dto.StudentSearchResponse;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.item.service.ItemService;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ItemService itemService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private FileStorageService fileStorageService;
    @MockitoBean
    private MemberDetailsService memberDetailsService;

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/items - 물품 등록")
    @Test
    void createItem() throws Exception {
        // Given
        ItemCreateForm form = ItemCreateForm.builder()
                .name("Sample Item")
                .description("Sample description")
                .price(2000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("No damage")
                .returnPolicy("Return in 3 days")
                .startDate(LocalDateTime.of(2025,1,1,9,0))
                .endDate(LocalDateTime.of(2025,1,8,18,0))
                .build();

        given(itemService.createItem(anyLong(), any(ItemCreateForm.class), anyList())).willReturn(42L);

        MockMultipartFile jsonPart = new MockMultipartFile(
                "form", "", "application/json",
                objectMapper.writeValueAsBytes(form));

        MockMultipartFile img1 = new MockMultipartFile(
                "images", "a.jpg", "image/jpeg", "dummy".getBytes());
        MockMultipartFile img2 = new MockMultipartFile(
                "images", "b.jpg", "image/jpeg", "dummy".getBytes());

        Student login = Student.builder().memberId(123L).role(MemberRoleEnum.STUDENT).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new MemberDetails(login), null, List.of());

        // When / Then
        mockMvc.perform(multipart("/api/v1/items")
                        .file(jsonPart)
                        .file(img1).file(img2)
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("create-item",
                        requestParts(
                                partWithName("form").description("ItemCreateForm JSON"),
                                partWithName("images").description("업로드 이미지 파일들 (1개 이상)")
                        ),
                        requestPartFields("form",
                                fieldWithPath("name").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("물품 상세 설명"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("물품 상태(`AVAILABLE`,`OUT` 등)"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("대여 가능 시작일(ISO-8601)"),
                                fieldWithPath("endDate").type(JsonFieldType.STRING).description("대여 가능 종료일(ISO-8601)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("등록된 물품의 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/items - 전체 물품 조회")
    @Test
    void getAllItems() throws Exception {
        // Given
        MemberSearchResponse owner = StudentSearchResponse.builder()
                .memberId(1001L).nickname("owner1")
                .profileImg("pfUrl").university("Korea Univ.").build();
        MemberSearchResponse owner2 = StudentSearchResponse.builder()
                .memberId(1002L).nickname("owner2")
                .profileImg("pfUrl2").university("Korea Univ.")
                .build();
        ItemSearchResponse dto = ItemSearchResponse.builder()
                .itemId(1L).owner(owner).name("One")
                .description("desc").price(1000)
                .imageUrls(List.of("url1", "url2"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp").returnPolicy("rp")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ItemSearchResponse dto2 = ItemSearchResponse.builder()
                .itemId(2L).owner(owner2).name("Two")
                .description("desc2").price(1000)
                .imageUrls(List.of("url3", "url4"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp2").returnPolicy("rp2")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemSearchResponse> page =
                new PageImpl<>(List.of(dto, dto2), pageable, 1);

        given(itemService.getAllItems(any(ItemSearchForm.class), any(Pageable.class)))
                .willReturn(page);

        // When / Then
        mockMvc.perform(get("/api/v1/items")
                        .with(csrf())
                        .queryParam("keyword", "가나다")
                        .queryParam("startDate", "2025-04-01T08:00:00")
                        .queryParam("endDate", "2025-05-01T20:00:00")
                        .queryParam("minPrice", "1000")
                        .queryParam("maxPrice", "3000")
                        .queryParam("status", "AVAILABLE")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("ownerRoles", "COMPANY", "COUNCIL")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("get-all-items",
                        relaxedQueryParameters(
                                parameterWithName("keyword").optional().description("검색 키워드 (물품명 또는 상세 설명 포함)"),
                                parameterWithName("startDate").optional().description("대여 가능 시작일 (ISO-8601 형식)"),
                                parameterWithName("endDate").optional().description("대여 가능 종료일 (ISO-8601 형식)"),
                                parameterWithName("minPrice").optional().description("최소 대여 가격"),
                                parameterWithName("maxPrice").optional().description("최대 대여 가격"),
                                parameterWithName("status").optional().description("물품 상태 필터 (예: AVAILABLE, OUT)"),
                                parameterWithName("ownerRoles").optional().description("물품 소유자 역할 필터 (예: STUDENT, COMPANY, COUNCIL; 여러 값 전달 가능)"),
                                parameterWithName("page").description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("sort").description("정렬 기준 (예: createdAt,desc)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.content[].owner.memberId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.content[].owner.profileImg").optional().type(JsonFieldType.STRING).description("소유자 프로필 이미지 URL"),
                                fieldWithPath("data.content[].owner.nickname").type(JsonFieldType.STRING).description("소유자 회원 닉네임"),
                                fieldWithPath("data.content[].owner.university").type(JsonFieldType.STRING).description("소유자 학생 소속 대학"),
                                fieldWithPath("data.content[].name").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("data.content[].imageUrls[]").type(JsonFieldType.ARRAY).description("물품 이미지 URL 리스트"),
                                fieldWithPath("data.content[].description").type(JsonFieldType.STRING).description("상세 설명"),
                                fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("물품 상태"),
                                fieldWithPath("data.content[].damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("data.content[].returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.content[].endDate").type(JsonFieldType.STRING).description("종료일"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("등록일"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("수정일"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지 (성공 시 빈 문자열)"),

                                fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("페이지 오프셋"),
                                fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
                                fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("비페이징 여부"),
                                fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("페이지 내 정렬 정보가 비어있는지 여부"),
                                fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("페이지 내 정렬이 적용되었는지 여부"),
                                fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("페이지 내 정렬이 미적용되었는지 여부"),

                                fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("전체 정렬 정보 비어있는지 여부"),
                                fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("전체 정렬 적용 여부"),
                                fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("전체 정렬 미적용 여부"),

                                fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("페이지가 비어있는지 여부")
                        )
                ));

        then(itemService).should().getAllItems(any(ItemSearchForm.class), any(Pageable.class));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/items/{id} - 단일 물품 조회")
    @Test
    void getItem() throws Exception {
        // Given
        long id = 5L;
        ItemSearchResponse dto = ItemSearchResponse.builder()
                .itemId(id)
                .owner(StudentSearchResponse.builder()
                        .memberId(1001L).profileImg("url1")
                        .nickname("owner").university("univ").build())
                .name("Single")
                .description("desc").price(1000)
                .imageUrls(List.of("urlA"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp").returnPolicy("rp")
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        given(itemService.getItem(id)).willReturn(dto);

        // When / Then
        mockMvc.perform(get("/api/v1/items/{id}", id)
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemId").value((int)id))
                .andDo(document("get-item",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.owner.memberId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.owner.profileImg").optional().type(JsonFieldType.STRING).description("소유자 프로필 이미지 URL"),
                                fieldWithPath("data.owner.nickname").type(JsonFieldType.STRING).description("소유자 회원 닉네임"),
                                fieldWithPath("data.owner.university").type(JsonFieldType.STRING).description("소유자 학생 소속 대학"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data.imageUrls[]").type(JsonFieldType.ARRAY).description("물품 이미지 URL 리스트"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명"),
                                fieldWithPath("data.price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.damagedPolicy").type(JsonFieldType.STRING).description("파손정책"),
                                fieldWithPath("data.returnPolicy").type(JsonFieldType.STRING).description("반납정책"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("종료일"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("물품 등록일"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("물품 정보 수정일"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/v1/items/{id} - 물품 수정")
    @Test
    void updateItem() throws Exception {
        long id = 7L;

        ItemUpdateForm form = ItemUpdateForm.builder()
                .name("Updated")
                .description("newDesc")
                .price(2000)
                .damagedPolicy("dp2")
                .returnPolicy("rp2")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        willDoNothing().given(itemService)
                .updateItem(any(MemberDto.class), eq(id),
                        any(ItemUpdateForm.class), anyList());

        // given
        Student login = Student.builder().memberId(123L).role(MemberRoleEnum.STUDENT).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new MemberDetails(login), null, List.of());

        MockMultipartFile jsonPart = new MockMultipartFile(
                "form", "", "application/json",
                objectMapper.writeValueAsBytes(form));

        MockMultipartFile img = new MockMultipartFile(
                "images", "z.jpg", "image/jpeg", "dummy".getBytes());

        // when
        ResultActions result = mockMvc.perform(multipart("/api/v1/items/{id}", id)
                .file(jsonPart).file(img)
                .with(request -> { request.setMethod("PUT"); return request; })
                .with(csrf()).with(authentication(auth)));


        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("update-item",
                        requestParts(
                                partWithName("form").description("ItemUpdateForm JSON"),
                                partWithName("images").optional()
                                        .description("교체/추가 이미지 파일 (0개 이상)")
                        ),
                        requestPartFields("form",
                                fieldWithPath("name").type(JsonFieldType.STRING).description("수정할 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("수정할 설명"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("수정할 시작일"),
                                fieldWithPath("endDate").type(JsonFieldType.STRING).description("수정할 종료일")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        // then: service 호출 검증
        verify(itemService).updateItem(any(MemberDto.class), eq(id), any(ItemUpdateForm.class), anyList());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/v1/items/{id} - 물품 삭제")
    @Test
    void deleteItem() throws Exception {
        //given
        long id = 9L;
        willDoNothing().given(itemService)
                .deleteItem(any(MemberDto.class), eq(id));

        Student login = Student.builder().memberId(321L).role(MemberRoleEnum.STUDENT).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new MemberDetails(login), null, List.of());

        // when
        ResultActions result = mockMvc.perform(delete("/api/v1/items/{id}", id)
                .with(csrf())
                .with(authentication(auth)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("delete-item",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        // then: service 호출 검증
        verify(itemService).deleteItem(any(MemberDto.class), eq(id));
    }

}