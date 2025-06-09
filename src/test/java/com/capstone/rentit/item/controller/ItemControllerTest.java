package com.capstone.rentit.item.controller;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.dto.*;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.dto.MemberSearchResponse;
import com.capstone.rentit.member.dto.StudentSearchResponse;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.item.service.ItemService;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    private MemberDetailsService memberDetailsService;

    @MockitoBean
    private FileStorageService fileStorageService;

    private Authentication authWith(long memberId) {
        var student = com.capstone.rentit.member.domain.Student.builder()
                .memberId(memberId)
                .role(MemberRoleEnum.STUDENT)
                .build();
        return new UsernamePasswordAuthenticationToken(new MemberDetails(student), null, List.of());
    }

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/items - 물품 등록")
    @Test
    void createItem() throws Exception {
        var form = ItemCreateForm.builder()
                .name("Sample Item")
                .description("Sample description")
                .damagedDescription("Sample damaged description")
                .price(2000)
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("No damage")
                .returnPolicy("Return in 3 days")
                .startDate(LocalDateTime.of(2025, 1, 1, 9, 0))
                .endDate(LocalDateTime.of(2025, 1, 8, 18, 0))
                .build();

        given(itemService.createItem(anyLong(), any(ItemCreateForm.class))).willReturn(42L);

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form))
                        .with(csrf())
                        .with(authentication(authWith(123L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("create-item",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("물품 상세 설명"),
                                fieldWithPath("damagedDescription").type(JsonFieldType.STRING).description("물품 하자 설명"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("물품 상태 (`AVAILABLE`, `OUT` 등)"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("대여 가능 시작일 (ISO-8601)"),
                                fieldWithPath("endDate").type(JsonFieldType.STRING).description("대여 가능 종료일 (ISO-8601)"),
                                fieldWithPath("imageKeys").optional().type(JsonFieldType.ARRAY).description("등록할 object key 리스트")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("등록된 물품의 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        then(itemService).should().createItem(anyLong(), any(ItemCreateForm.class));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/items - 전체 물품 조회")
    @Test
    void getAllItems() throws Exception {
        MemberSearchResponse owner1 = StudentSearchResponse.builder()
                .memberId(1001L).nickname("owner1")
                .profileImg("pfUrl").university("Korea Univ.").build();
        MemberSearchResponse owner2 = StudentSearchResponse.builder()
                .memberId(1002L).nickname("owner2")
                .profileImg("pfUrl2").university("Korea Univ.").build();

        var dto1 = ItemSearchResponse.builder()
                .itemId(1L).owner(owner1).name("One")
                .description("desc").price(1000)
                .damagedDescription("물품 하자 정보 1")
                .imageKeys(List.of("key1", "key2"))
                .imageUrls(List.of("url1", "url2"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp").returnPolicy("rp")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        var dto2 = ItemSearchResponse.builder()
                .itemId(2L).owner(owner2).name("Two")
                .description("desc2").price(1000)
                .damagedDescription("물품 하자 정보 2")
                .imageKeys(List.of("key3"))
                .imageUrls(List.of("url3", "url4"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp2").returnPolicy("rp2")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<ItemSearchResponse> page = new PageImpl<>(List.of(dto1, dto2), pageable, 2);

        given(itemService.getAllItems(any(ItemSearchForm.class), any(Pageable.class))).willReturn(page);

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
                        .queryParam("university", "아주대학교"))
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
                                parameterWithName("ownerRoles").optional().description("소유자 역할 필터 (예: STUDENT, COMPANY, COUNCIL)"),
                                parameterWithName("university").optional().description("대학교 필터"),
                                parameterWithName("page").description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("sort").description("정렬 기준 (예: createdAt,desc)")
                        ),
                        relaxedResponseFields(
                                // 기본 응답 구조
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.content[].owner.memberId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.content[].owner.profileImg").type(JsonFieldType.STRING).description("소유자 프로필 이미지 URL"),
                                fieldWithPath("data.content[].owner.nickname").type(JsonFieldType.STRING).description("소유자 회원 닉네임"),
                                fieldWithPath("data.content[].owner.university").type(JsonFieldType.STRING).description("소유자 소속 대학"),
                                fieldWithPath("data.content[].name").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("data.content[].imageKeys").type(JsonFieldType.ARRAY).description("이미지 object key 리스트"),
                                fieldWithPath("data.content[].imageUrls[]").type(JsonFieldType.ARRAY).description("물품 이미지 URL 리스트"),
                                fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER).description("대여 가격"),

                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                                fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                                fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                                fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("페이지가 비어있는지 여부"),

                                fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어있는지 여부"),
                                fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬 적용 여부"),
                                fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬 미적용 여부"),

                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지 (성공 시 빈 문자열)")
                        )
                ));

        then(itemService).should().getAllItems(any(ItemSearchForm.class), any(Pageable.class));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/items/{id} - 단일 물품 조회")
    @Test
    void getItem() throws Exception {
        long id = 5L;
        var dto = ItemSearchResponse.builder()
                .itemId(id)
                .owner(StudentSearchResponse.builder()
                        .memberId(1001L).profileImg("url1")
                        .nickname("owner").university("univ").build())
                .name("Single")
                .description("desc").price(1000)
                .damagedDescription("물품 하자 정보")
                .imageKeys(List.of("keyA"))
                .imageUrls(List.of("urlA"))
                .status(ItemStatusEnum.AVAILABLE)
                .damagedPolicy("dp").returnPolicy("rp")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(itemService.getItem(id)).willReturn(dto);

        mockMvc.perform(get("/api/v1/items/{id}", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemId").value((int) id))
                .andDo(document("get-item",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.itemId").type(JsonFieldType.NUMBER).description("물품 ID"),
                                fieldWithPath("data.owner.memberId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.owner.profileImg").optional().type(JsonFieldType.STRING).description("소유자 프로필 이미지 URL"),
                                fieldWithPath("data.owner.nickname").type(JsonFieldType.STRING).description("소유자 회원 닉네임"),
                                fieldWithPath("data.owner.university").type(JsonFieldType.STRING).description("소유자 학생 소속 대학"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data.imageKeys").optional().type(JsonFieldType.ARRAY).description("이미지 object key 리스트"),
                                fieldWithPath("data.imageUrls[]").type(JsonFieldType.ARRAY).description("물품 이미지 URL 리스트"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명"),
                                fieldWithPath("data.damagedDescription").type(JsonFieldType.STRING).description("물품 하자 설명"),
                                fieldWithPath("data.price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("data.returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작일"),
                                fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("종료일"),
                                fieldWithPath("data.rentalEndAt").type(JsonFieldType.NULL).description("status가 OUT인 경우 대여 마감일 표시"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("등록일"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("수정일"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        then(itemService).should().getItem(id);
    }

    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/v1/items/{id} - 물품 수정")
    @Test
    void updateItem() throws Exception {
        long id = 7L;
        var form = ItemUpdateForm.builder()
                .name("Updated")
                .description("newDesc")
                .damagedDescription("newDamagedDesc")
                .price(2000)
                .damagedPolicy("dp2")
                .returnPolicy("rp2")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        willDoNothing().given(itemService)
                .updateItem(any(com.capstone.rentit.member.dto.MemberDto.class), eq(id), any(ItemUpdateForm.class));

        mockMvc.perform(put("/api/v1/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form))
                        .with(csrf())
                        .with(authentication(authWith(123L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("update-item",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("수정할 이름"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("수정할 상세 설명"),
                                fieldWithPath("damagedDescription").type(JsonFieldType.STRING).description("수정할 하자 설명"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("수정할 대여 가능 시작일"),
                                fieldWithPath("endDate").type(JsonFieldType.STRING).description("수정할 대여 가능 종료일"),
                                fieldWithPath("imageKeys").optional().type(JsonFieldType.ARRAY).description("교체할 object key 리스트")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        verify(itemService).updateItem(any(com.capstone.rentit.member.dto.MemberDto.class), eq(id), any(ItemUpdateForm.class));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/v1/items/{id} - 물품 삭제")
    @Test
    void deleteItem() throws Exception {
        long id = 9L;
        willDoNothing().given(itemService)
                .deleteItem(any(com.capstone.rentit.member.dto.MemberDto.class), eq(id));

        mockMvc.perform(delete("/api/v1/items/{id}", id)
                        .with(csrf())
                        .with(authentication(authWith(321L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("delete-item",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));

        verify(itemService).deleteItem(any(com.capstone.rentit.member.dto.MemberDto.class), eq(id));
    }
}
