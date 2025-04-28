package com.capstone.rentit.item.controller;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemDto;
import com.capstone.rentit.item.dto.ItemSearchForm;
import com.capstone.rentit.item.dto.ItemUpdateForm;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
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
    private MemberDetailsService memberDetailsService;

    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/items - 물품 등록")
    @Test
    void createItem() throws Exception {
        // Given
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(1L);
        form.setName("Sample Item");
        form.setItemImg("http://example.com/item.jpg");
        form.setDescription("Sample description");
        form.setCategoryId(1L);
        form.setPrice(2000);
        form.setStatus(ItemStatusEnum.AVAILABLE);
        form.setDamagedPolicy("No damage allowed");
        form.setReturnPolicy("Return within 3 days");
        form.setStartDate(LocalDateTime.of(2025,1,1,9,0));
        form.setEndDate(LocalDateTime.of(2025,1,8,18,0));

        when(itemService.createItem(any(ItemCreateForm.class))).thenReturn(42L);

        // When / Then
        mockMvc.perform(post("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(42))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("create-item",
                        requestFields(
                                fieldWithPath("ownerId").type(JsonFieldType.NUMBER).description("물품 소유자 ID"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("물품 이름"),
                                fieldWithPath("itemImg").type(JsonFieldType.STRING).description("물품 이미지 URL"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("물품 상세 설명"),
                                fieldWithPath("categoryId").type(JsonFieldType.NUMBER).description("물품 카테고리 ID"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("물품 상태"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("파손 정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("반납 정책"),
                                fieldWithPath("startDate").type(JsonFieldType.STRING).description("대여 가능 시작일 (ISO)"),
                                fieldWithPath("endDate").type(JsonFieldType.STRING).description("대여 가능 종료일 (ISO)")
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
        ItemDto dto1 = ItemDto.builder()
                .itemId(1L).ownerId(1L).name("One").itemImg("url1").description("desc1")
                .categoryId(1L).status(ItemStatusEnum.AVAILABLE).damagedPolicy("p1").returnPolicy("r1").price(1000)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        ItemDto dto2 = ItemDto.builder()
                .itemId(2L).ownerId(2L).name("Two").itemImg("url2").description("desc2")
                .categoryId(2L).status(ItemStatusEnum.OUT).damagedPolicy("p2").returnPolicy("r2").price(2000)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        List<ItemDto> list = Arrays.asList(dto1, dto2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<ItemDto> page = new PageImpl<>(list, pageable, 2);
        given(itemService.getAllItems(any(ItemSearchForm.class), any(Pageable.class)))
                .willReturn(page);

        // When / Then
        mockMvc.perform(get("/api/v1/items")
                        .with(csrf())
                        .queryParam("keyword", "")
                        .queryParam("startDate", "")
                        .queryParam("endDate", "")
                        .queryParam("minPrice", "")
                        .queryParam("maxPrice", "")
                        .queryParam("status", "")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(document("get-all-items",
                        relaxedQueryParameters(
                                parameterWithName("keyword").optional()
                                        .description("검색 키워드 (물품명 또는 상세 설명 포함)"),
                                parameterWithName("startDate").optional()
                                        .description("대여 가능 시작일 (ISO-8601 형식)"),
                                parameterWithName("endDate").optional()
                                        .description("대여 가능 종료일 (ISO-8601 형식)"),
                                parameterWithName("minPrice").optional()
                                        .description("최소 대여 가격"),
                                parameterWithName("maxPrice").optional()
                                        .description("최대 대여 가격"),
                                parameterWithName("status").optional()
                                        .description("물품 상태 필터 (예: AVAILABLE, OUT)"),
                                parameterWithName("page")
                                        .description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size")
                                        .description("페이지 크기"),
                                parameterWithName("sort")
                                        .description("정렬 기준 (예: createdAt,desc)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("API 호출 성공 여부"),
                                fieldWithPath("data.content[].itemId").type(JsonFieldType.NUMBER)
                                        .description("물품 ID"),
                                fieldWithPath("data.content[].ownerId").type(JsonFieldType.NUMBER)
                                        .description("소유자 ID"),
                                fieldWithPath("data.content[].name").type(JsonFieldType.STRING)
                                        .description("물품 이름"),
                                fieldWithPath("data.content[].itemImg").type(JsonFieldType.STRING)
                                        .description("이미지 URL"),
                                fieldWithPath("data.content[].description").type(JsonFieldType.STRING)
                                        .description("상세 설명"),
                                fieldWithPath("data.content[].categoryId").type(JsonFieldType.NUMBER)
                                        .description("카테고리 ID"),
                                fieldWithPath("data.content[].price").type(JsonFieldType.NUMBER)
                                        .description("대여 가격"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                        .description("물품 상태"),
                                fieldWithPath("data.content[].damagedPolicy").type(JsonFieldType.STRING)
                                        .description("파손 정책"),
                                fieldWithPath("data.content[].returnPolicy").type(JsonFieldType.STRING)
                                        .description("반납 정책"),
                                fieldWithPath("data.content[].startDate").type(JsonFieldType.STRING)
                                        .description("시작일"),
                                fieldWithPath("data.content[].endDate").type(JsonFieldType.STRING)
                                        .description("종료일"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING)
                                        .description("등록일"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING)
                                        .description("수정일"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("응답 메시지 (성공 시 빈 문자열)"),

                                fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                                fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("페이지 오프셋"),
                                fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
                                fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("비페이징 여부"),
                                fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN)
                                        .description("페이지 내 정렬 정보가 비어있는지 여부"),
                                fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN)
                                        .description("페이지 내 정렬이 적용되었는지 여부"),
                                fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                        .description("페이지 내 정렬이 미적용되었는지 여부"),

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
        ItemDto dto = ItemDto.builder()
                .itemId(id).ownerId(1L).name("Single").itemImg("url")
                .description("desc").categoryId(1L).status(ItemStatusEnum.AVAILABLE).price(1000)
                .damagedPolicy("dp").returnPolicy("rp")
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(itemService.getItem(id)).thenReturn(dto);

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
                                fieldWithPath("data.ownerId").type(JsonFieldType.NUMBER).description("소유자 ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data.itemImg").type(JsonFieldType.STRING).description("이미지"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명"),
                                fieldWithPath("data.categoryId").type(JsonFieldType.NUMBER).description("카테고리"),
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
        ItemUpdateForm form = new ItemUpdateForm();
        form.setName("Updated");
        form.setItemImg("newUrl");
        form.setDescription("newDesc");
        form.setCategoryId(2L);
        form.setPrice(2000);
        form.setDamagedPolicy("dp2");
        form.setReturnPolicy("rp2");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(2));

        // given
        Student loginMember = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .role(MemberRoleEnum.STUDENT)
                .build();
        MemberDetails details = new MemberDetails(loginMember);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        when(itemService.getItem(id)).thenReturn(
                ItemDto.builder()
                        .itemId(id)
                        .ownerId(id)
                        .build()
        );
        doNothing().when(itemService)
                .updateItem(any(MemberDto.class), eq(id), any(ItemUpdateForm.class));

        // when
        ResultActions result = mockMvc.perform(put("/api/v1/items/{id}", id)
                .with(csrf())
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("update-item",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("수정할 이름"),
                                fieldWithPath("itemImg").type(JsonFieldType.STRING).description("수정할 이미지"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("수정할 설명"),
                                fieldWithPath("categoryId").type(JsonFieldType.NUMBER).description("수정할 카테고리"),
                                fieldWithPath("price").type(JsonFieldType.NUMBER).description("대여 가격"),
                                fieldWithPath("damagedPolicy").type(JsonFieldType.STRING).description("수정할 파손정책"),
                                fieldWithPath("returnPolicy").type(JsonFieldType.STRING).description("수정할 반납정책"),
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
        verify(itemService).updateItem(any(MemberDto.class), eq(id), any(ItemUpdateForm.class));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/v1/items/{id} - 물품 삭제")
    @Test
    void deleteItem() throws Exception {
        long id = 9L;

        // given
        Student loginMember = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .role(MemberRoleEnum.STUDENT)
                .build();
        MemberDetails details = new MemberDetails(loginMember);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        when(itemService.getItem(id)).thenReturn(
                ItemDto.builder()
                        .itemId(id)
                        .ownerId(id)
                        .build()
        );
        doNothing().when(itemService).deleteItem(any(MemberDto.class), eq(id));

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