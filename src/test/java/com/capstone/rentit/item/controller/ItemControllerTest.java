package com.capstone.rentit.item.controller;

import com.capstone.rentit.item.dto.ItemCreateForm;
import com.capstone.rentit.item.dto.ItemUpdateForm;
import com.capstone.rentit.item.repository.ItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(roles = "USER")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * POST /api/v1/items - 물품 등록 테스트 (REST Docs 포함)
     */
    @Test
    public void testCreateItem() throws Exception {
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(1L);
        form.setName("Sample Item");
        form.setItemImg("http://example.com/item.jpg");
        form.setDescription("Sample description");
        form.setCategoryId(1L);
        form.setStatus(0);
        form.setDamagedPolicy("No damage allowed");
        form.setReturnPolicy("Return within 3 days");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(7));

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // 등록된 물품의 id가 응답 data로 반환됨
                .andExpect(jsonPath("$.data").exists())
                .andDo(document("create_item",
                        requestFields(
                                fieldWithPath("ownerId").description("물품 소유자 ID"),
                                fieldWithPath("name").description("물품 이름"),
                                fieldWithPath("itemImg").description("물품 이미지 URL"),
                                fieldWithPath("description").description("물품 상세 설명"),
                                fieldWithPath("categoryId").description("물품 카테고리 ID"),
                                fieldWithPath("status").description("물품 상태 (정수값)"),
                                fieldWithPath("damagedPolicy").description("파손 정책"),
                                fieldWithPath("returnPolicy").description("반납 정책"),
                                fieldWithPath("startDate").description("대여 가능 시작일 (ISO 형식)"),
                                fieldWithPath("endDate").description("대여 가능 종료일 (ISO 형식)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("등록된 물품의 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * GET /api/v1/items - 전체 물품 조회 테스트 (REST Docs 포함)
     */
    @Test
    public void testGetAllItems() throws Exception {
        // 사전 등록: 2개 물품 등록 (생성 API를 호출)
        ItemCreateForm form1 = new ItemCreateForm();
        form1.setOwnerId(1L);
        form1.setName("Item One");
        form1.setItemImg("http://example.com/1.jpg");
        form1.setDescription("Description One");
        form1.setCategoryId(1L);
        form1.setStatus(0);
        form1.setDamagedPolicy("Policy One");
        form1.setReturnPolicy("Return One");
        form1.setStartDate(LocalDateTime.now());
        form1.setEndDate(LocalDateTime.now().plusDays(5));

        ItemCreateForm form2 = new ItemCreateForm();
        form2.setOwnerId(2L);
        form2.setName("Item Two");
        form2.setItemImg("http://example.com/2.jpg");
        form2.setDescription("Description Two");
        form2.setCategoryId(2L);
        form2.setStatus(1);
        form2.setDamagedPolicy("Policy Two");
        form2.setReturnPolicy("Return Two");
        form2.setStartDate(LocalDateTime.now());
        form2.setEndDate(LocalDateTime.now().plusDays(7));

        String json1 = objectMapper.writeValueAsString(form1);
        String json2 = objectMapper.writeValueAsString(form2);

        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(document("get_all_items",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("물품 목록").type(JsonFieldType.ARRAY),
                                fieldWithPath("data[].itemId").description("물품 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].ownerId").description("물품 소유자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].name").description("물품 이름").type(JsonFieldType.STRING),
                                fieldWithPath("data[].itemImg").description("물품 이미지 URL").type(JsonFieldType.STRING),
                                fieldWithPath("data[].description").description("물품 상세 설명").type(JsonFieldType.STRING),
                                fieldWithPath("data[].categoryId").description("물품 카테고리 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].status").description("물품 상태").type(JsonFieldType.STRING),
                                fieldWithPath("data[].damagedPolicy").description("파손 정책").type(JsonFieldType.STRING),
                                fieldWithPath("data[].returnPolicy").description("반납 정책").type(JsonFieldType.STRING),
                                fieldWithPath("data[].startDate").description("대여 가능 시작일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data[].endDate").description("대여 가능 종료일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data[].createdAt").description("물품 등록일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data[].updatedAt").description("물품 수정일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * GET /api/v1/items/{itemId} - 단일 물품 조회 테스트 (REST Docs 포함)
     */
    @Test
    public void testGetItem() throws Exception {
        // 사전 등록: 물품 생성 API 호출로 등록
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(3L);
        form.setName("Single Item");
        form.setItemImg("http://example.com/single.jpg");
        form.setDescription("Single description");
        form.setCategoryId(3L);
        form.setStatus(0);
        form.setDamagedPolicy("Single policy");
        form.setReturnPolicy("Single return");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(3));

        String createJson = objectMapper.writeValueAsString(form);
        String createResponse = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode createNode = objectMapper.readTree(createResponse);
        Long itemId = createNode.get("data").asLong();

        mockMvc.perform(get("/api/v1/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemId").value(itemId.intValue()))
                .andDo(document("get_item",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.itemId").description("물품 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.ownerId").description("물품 소유자 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.name").description("물품 이름").type(JsonFieldType.STRING),
                                fieldWithPath("data.itemImg").description("물품 이미지 URL").type(JsonFieldType.STRING),
                                fieldWithPath("data.description").description("물품 상세 설명").type(JsonFieldType.STRING),
                                fieldWithPath("data.categoryId").description("물품 카테고리 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.status").description("물품 상태").type(JsonFieldType.STRING),
                                fieldWithPath("data.damagedPolicy").description("파손 정책").type(JsonFieldType.STRING),
                                fieldWithPath("data.returnPolicy").description("반납 정책").type(JsonFieldType.STRING),
                                fieldWithPath("data.startDate").description("대여 가능 시작일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data.endDate").description("대여 가능 종료일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data.createdAt").description("물품 등록일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("data.updatedAt").description("물품 수정일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * PUT /api/v1/{itemId} - 물품 정보 수정 테스트 (REST Docs 포함)
     */
    @Test
    public void testUpdateItem() throws Exception {
        // 사전 등록: 물품 생성 후 수정 API 호출
        ItemCreateForm createForm = new ItemCreateForm();
        createForm.setOwnerId(4L);
        createForm.setName("Item to Update");
        createForm.setItemImg("http://example.com/update.jpg");
        createForm.setDescription("Original description");
        createForm.setCategoryId(4L);
        createForm.setStatus(0);
        createForm.setDamagedPolicy("Original policy");
        createForm.setReturnPolicy("Original return");
        createForm.setStartDate(LocalDateTime.now());
        createForm.setEndDate(LocalDateTime.now().plusDays(4));

        String createJson = objectMapper.writeValueAsString(createForm);
        String createResponse = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode createNode = objectMapper.readTree(createResponse);
        Long itemId = createNode.get("data").asLong();

        // 업데이트를 위한 폼 생성
        ItemUpdateForm updateForm = new ItemUpdateForm();
        updateForm.setName("Updated Item Name");
        updateForm.setItemImg("http://example.com/updated.jpg");
        updateForm.setDescription("Updated description");
        updateForm.setCategoryId(5L);
        updateForm.setStatus(1);
        updateForm.setDamagedPolicy("Updated policy");
        updateForm.setReturnPolicy("Updated return");
        updateForm.setStartDate(LocalDateTime.now().plusDays(1));
        updateForm.setEndDate(LocalDateTime.now().plusDays(5));

        String updateJson = objectMapper.writeValueAsString(updateForm);

        // PUT 매핑은 "/api/v1/{itemId}" 로 매핑되어 있음
        mockMvc.perform(put("/api/v1/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("update_item",
                        requestFields(
                                fieldWithPath("name").description("수정할 물품 이름").type(JsonFieldType.STRING),
                                fieldWithPath("itemImg").description("수정할 물품 이미지 URL").type(JsonFieldType.STRING),
                                fieldWithPath("description").description("수정할 물품 상세 설명").type(JsonFieldType.STRING),
                                fieldWithPath("categoryId").description("수정할 물품 카테고리 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("status").description("수정할 물품 상태").type(JsonFieldType.NUMBER),
                                fieldWithPath("damagedPolicy").description("수정할 파손 정책").type(JsonFieldType.STRING),
                                fieldWithPath("returnPolicy").description("수정할 반납 정책").type(JsonFieldType.STRING),
                                fieldWithPath("startDate").description("수정할 대여 가능 시작일 (ISO 형식)").type(JsonFieldType.STRING),
                                fieldWithPath("endDate").description("수정할 대여 가능 종료일 (ISO 형식)").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("수정된 항목은 없으므로 null 반환").type(JsonFieldType.NULL),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }

    /**
     * DELETE /api/v1/{itemId} - 물품 삭제 테스트 (REST Docs 포함)
     */
    @Test
    public void testDeleteItem() throws Exception {
        // 사전 등록: 물품 생성 후 삭제 테스트
        ItemCreateForm form = new ItemCreateForm();
        form.setOwnerId(5L);
        form.setName("Item to Delete");
        form.setItemImg("http://example.com/delete.jpg");
        form.setDescription("Delete description");
        form.setCategoryId(5L);
        form.setStatus(0);
        form.setDamagedPolicy("Delete policy");
        form.setReturnPolicy("Delete return");
        form.setStartDate(LocalDateTime.now());
        form.setEndDate(LocalDateTime.now().plusDays(2));

        String createJson = objectMapper.writeValueAsString(form);
        String createResponse = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode createNode = objectMapper.readTree(createResponse);
        Long itemId = createNode.get("data").asLong();

        mockMvc.perform(delete("/api/v1/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("delete_item",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("삭제 후 데이터는 null").type(JsonFieldType.NULL),
                                fieldWithPath("message").description("성공 시 빈 문자열, 실패 시 에러 메시지").type(JsonFieldType.STRING)
                        )
                ));
    }
}