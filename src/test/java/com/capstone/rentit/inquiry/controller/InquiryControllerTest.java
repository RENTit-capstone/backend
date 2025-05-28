package com.capstone.rentit.inquiry.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.inquiry.dto.*;
import com.capstone.rentit.inquiry.service.InquiryService;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InquiryController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class InquiryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InquiryService inquiryService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private FileStorageService fileStorageService;

    @BeforeEach
    void bypassSecurityFilter() throws Exception {
        doAnswer(invocation -> {
            var req = invocation.getArgument(0, HttpServletRequest.class);
            var res = invocation.getArgument(1, HttpServletResponse.class);
            var chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    /** 공통 helper **/
    private Authentication authFor(long memberId, MemberRoleEnum role) {
        Student user = Student.builder().memberId(memberId).role(role).build();
        MemberDetails details = new MemberDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private FieldDescriptor[] commonSuccessFields(String type, String desc) {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                fieldWithPath("data").type(JsonFieldType.valueOf(type)).description(desc),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
        };
    }

    private FieldDescriptor[] commonInquiryFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                fieldWithPath("data.inquiryId").type(JsonFieldType.NUMBER).description("문의 ID"),
                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("작성자 ID"),
                fieldWithPath("data.type").type(JsonFieldType.STRING).description("문의 타입"),
                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                fieldWithPath("data.content").type(JsonFieldType.STRING).description("내용"),
                fieldWithPath("data.answer").type(JsonFieldType.STRING).description("답변 내용").optional(),
                fieldWithPath("data.images").type(JsonFieldType.ARRAY).description("파손 신고 이미지 URL 리스트").optional(),
                fieldWithPath("data.processed").type(JsonFieldType.BOOLEAN).description("처리 여부"),
                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각 (ISO-8601)"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
        };
    }

    @Nested
    @DisplayName("USER API")
    class UserApi {
        @Test
        @WithMockUser(roles = "USER")
        void createInquiry() throws Exception {
            InquiryCreateForm form = new InquiryCreateForm("title", "content", InquiryType.SERVICE);
            given(inquiryService.createInquiry(anyLong(), any())).willReturn(1L);

            mockMvc.perform(post("/api/v1/inquiries")
                            .with(csrf())
                            .with(authentication(authFor(100L, MemberRoleEnum.STUDENT)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(1L))
                    .andDo(document("user-create-inquiry",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            requestFields(
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("문의 제목"),
                                    fieldWithPath("content").type(JsonFieldType.STRING).description("문의 내용"),
                                    fieldWithPath("type").type(JsonFieldType.STRING).description("문의 타입")
                            ),
                            responseFields(commonSuccessFields("NUMBER", "생성된 문의 ID"))
                    ));
        }

        @Test
        @WithMockUser(roles = "USER")
        void createDamageReport() throws Exception {
            DamageReportCreateForm form = new DamageReportCreateForm(
                    3L, "파손 신고 제목", "파손 신고 내용", List.of("/img1.png", "/img2.png")
            );
            given(inquiryService.createDamageReport(anyLong(), any())).willReturn(99L);

            mockMvc.perform(post("/api/v1/inquiries/damage")
                            .with(csrf())
                            .with(authentication(authFor(100L, MemberRoleEnum.STUDENT)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(99L))
                    .andDo(document("user-create-damage",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            requestFields(
                                    fieldWithPath("rentalId").type(JsonFieldType.NUMBER).description("대여 건 ID"),
                                    fieldWithPath("title").type(JsonFieldType.STRING).description("파손 신고 제목"),
                                    fieldWithPath("content").type(JsonFieldType.STRING).description("파손 신고 내용"),
                                    fieldWithPath("images").type(JsonFieldType.ARRAY).description("파손 이미지 URL 리스트")
                            ),
                            responseFields(commonSuccessFields("NUMBER", "생성된 신고 ID"))
                    ));
        }

        @Test
        @WithMockUser(roles = "USER")
        void answerDamageReport() throws Exception {
            long id = 77L;
            InquiryAnswerForm form = new InquiryAnswerForm("사용자 답변");
            doNothing().when(inquiryService).answerDamageReport(eq(id), anyLong(), any());

            mockMvc.perform(put("/api/v1/inquiries/{id}/answer", id)
                            .with(csrf())
                            .with(authentication(authFor(100L, MemberRoleEnum.STUDENT)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(nullValue()))
                    .andDo(document("user-answer-damage",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            pathParameters(parameterWithName("id").description("신고 ID")),
                            requestFields(fieldWithPath("answer").type(JsonFieldType.STRING).description("답변 내용")),
                            responseFields(commonSuccessFields("NULL", "항상 null"))
                    ));
        }
    }

    @Nested
    @DisplayName("ADMIN API")
    class AdminApi {
        @Test
        @WithMockUser(roles = "ADMIN")
        void findInquiry() throws Exception {
            InquiryResponse dto = new InquiryResponse(10L, 100L, InquiryType.SERVICE, "t", "c", null, false, LocalDateTime.now());
            given(inquiryService.getInquiry(10L)).willReturn(dto);

            mockMvc.perform(get("/api/v1/admin/inquiries/{id}", 10L)
                            .with(authentication(authFor(999L, MemberRoleEnum.ADMIN))))
                    .andExpect(status().isOk())
                    .andDo(document("admin-find-inquiry",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            pathParameters(parameterWithName("id").description("문의 ID")),
                            responseFields(commonInquiryFields())
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void searchInquiries() throws Exception {
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
            InquiryResponse sample = new InquiryResponse(30L, 3L, InquiryType.REPORT, "TITLE", "CONTENT", null, true, LocalDateTime.now());
            Page<InquiryResponse> page = new PageImpl<>(List.of(sample), pageable, 1);
            given(inquiryService.search(any(), eq(MemberRoleEnum.ADMIN), anyLong(), eq(pageable))).willReturn(page);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("type", "REPORT");
            params.add("processed", "true");
            params.add("page", "0");
            params.add("size", "5");

            mockMvc.perform(get("/api/v1/admin/inquiries")
                            .params(params)
                            .with(authentication(authFor(999L, MemberRoleEnum.ADMIN))))
                    .andExpect(status().isOk())
                    .andDo(document("admin-search-inquiries",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            relaxedQueryParameters(
                                    parameterWithName("type").description("문의 타입"),
                                    parameterWithName("processed").description("처리 여부"),
                                    parameterWithName("page").description("페이지 번호"),
                                    parameterWithName("size").description("페이지 크기")
                            ),
                            relaxedResponseFields(
                                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                    subsectionWithPath("data").description("페이지 정보 및 문의 리스트 (content, pageable 등)"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void answerInquiry() throws Exception {
            long id = 70L;
            InquiryAnswerForm form = new InquiryAnswerForm("관리자 답변");
            doNothing().when(inquiryService).answerInquiry(eq(id), any());

            mockMvc.perform(put("/api/v1/admin/inquiries/{id}/answer", id)
                            .with(csrf())
                            .with(authentication(authFor(999L, MemberRoleEnum.ADMIN)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(form)))
                    .andExpect(status().isOk())
                    .andDo(document("admin-answer-inquiry",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            pathParameters(parameterWithName("id").description("문의 ID")),
                            requestFields(fieldWithPath("answer").type(JsonFieldType.STRING).description("답변 내용")),
                            responseFields(commonSuccessFields("NULL", "항상 null"))
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void markProcessed() throws Exception {
            doNothing().when(inquiryService).markProcessed(55L);

            mockMvc.perform(put("/api/v1/admin/inquiries/{id}/processed", 55L)
                            .with(csrf())
                            .with(authentication(authFor(999L, MemberRoleEnum.ADMIN))))
                    .andExpect(status().isOk())
                    .andDo(document("admin-mark-processed",
                            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                            pathParameters(parameterWithName("id").description("문의 ID")),
                            responseFields(commonSuccessFields("NULL", "항상 null"))
                    ));
        }
    }
}