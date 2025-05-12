package com.capstone.rentit.inquiry.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.inquiry.dto.InquiryAnswerForm;
import com.capstone.rentit.inquiry.dto.InquiryCreateForm;
import com.capstone.rentit.inquiry.dto.InquiryResponse;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.inquiry.service.InquiryService;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InquiryController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ExtendWith(SpringExtension.class)
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("POST /api/v1/inquiries - 사용자 문의 생성")
    @WithMockUser(roles = "USER")
    void createInquiry() throws Exception {
        given(inquiryService.createInquiry(any(InquiryCreateForm.class))).willReturn(100L);
        var form = new InquiryCreateForm(1L, "제목1", "내용1", InquiryType.SERVICE);

        mockMvc.perform(post("/api/v1/inquiries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100L))
                .andDo(document("create-inquiry",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        requestFields(
                                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("작성자 ID"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("문의 제목"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("문의 내용"),
                                fieldWithPath("type").type(JsonFieldType.STRING).description("문의 타입")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 문의 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/inquiries/{id} - 사용자 단일 문의 조회")
    @WithMockUser(roles = "USER")
    void findInquiryForUser() throws Exception {
        var resp = new InquiryResponse(10L,1L,InquiryType.REPORT,"Q","A",true,LocalDateTime.now());
        given(inquiryService.getInquiry(10L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/inquiries/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inquiryId").value(10L))
                .andDo(document("user-find-inquiry",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("문의 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.inquiryId").type(JsonFieldType.NUMBER).description("문의 ID"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("문의 요청 사용자 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("문의 타입"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("문의 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("문의 내용"),
                                fieldWithPath("data.processed").type(JsonFieldType.BOOLEAN).description("처리 여부"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각 (ISO-8601)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/inquiries - 사용자 타입별 문의 조회")
    @WithMockUser(roles = "USER")
    void searchForUser() throws Exception {
        var list = List.of(new InquiryResponse(11L,1L,InquiryType.SERVICE,"Hi","Hello",false,LocalDateTime.now()));
        given(inquiryService.getInquiries(1000L, InquiryType.SERVICE)).willReturn(list);

        Student student = Student.builder()
                .memberId(1000L).email("login@student.com")
                .name("Login Student").nickname("loginNick")
                .phone("010-1111-1111").university("Test University")
                .studentId("S12345678").gender(GenderEnum.MEN)
                .role(MemberRoleEnum.STUDENT).locked(false)
                .createdAt(LocalDate.of(2025, 4, 16))
                .build();

        MemberDetails details = new MemberDetails(student);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );

        mockMvc.perform(get("/api/v1/inquiries")
                        .param("type","SERVICE")
                        .with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].inquiryId").value(11L))
                .andDo(document("user-search-inquiries",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        RequestDocumentation.queryParameters(
                                parameterWithName("type").description("문의 타입")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data[].inquiryId").type(JsonFieldType.NUMBER).description("문의 ID"),
                                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("작성자 ID"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING).description("문의 타입"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("문의 제목"),
                                fieldWithPath("data[].content").type(JsonFieldType.STRING).description("문의 내용"),
                                fieldWithPath("data[].processed").type(JsonFieldType.BOOLEAN).description("처리 여부"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("작성 시각 (ISO-8601)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inquiries/{id} - 관리자 단일 조회")
    @WithMockUser(roles = "ADMIN")
    void findInquiryForAdmin() throws Exception {
        var resp = new InquiryResponse(20L,2L,InquiryType.SERVICE,"T","C",false,LocalDateTime.now());
        given(inquiryService.getInquiry(20L)).willReturn(resp);

        mockMvc.perform(get("/api/v1/admin/inquiries/{id}",20L))
                .andExpect(status().isOk())
                .andDo(document("admin-find-inquiry",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("문의 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.inquiryId").type(JsonFieldType.NUMBER).description("문의 ID"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("문의 요청 사용자 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("문의 타입"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("문의 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("문의 내용"),
                                fieldWithPath("data.processed").type(JsonFieldType.BOOLEAN).description("처리 여부"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 시각 (ISO-8601)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inquiries - 관리자 페이징 검색")
    @WithMockUser(roles = "ADMIN")
    void searchForAdmin() throws Exception {
        var form = new InquirySearchForm(InquiryType.REPORT, true, null, null);
        Pageable pageable = PageRequest.of(0,5,Sort.by("createdAt").descending());
        Page<InquiryResponse> page = new PageImpl<>(List.of(
                new InquiryResponse(30L,3L,InquiryType.REPORT,"X","Y",true,LocalDateTime.now())
        ), pageable, 1);
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("type","REPORT");
        params.add("processed","true");
        params.add("page","0");
        params.add("size","5");
        given(inquiryService.search(form, pageable)).willReturn(page);

        mockMvc.perform(get("/api/v1/admin/inquiries").params(params))
                .andExpect(status().isOk())
                .andDo(document("admin-search-inquiries",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        RequestDocumentation.queryParameters(
                                parameterWithName("type").description("문의 타입"),
                                parameterWithName("processed").description("처리 여부"),
                                parameterWithName("page").description("페이지 번호"),
                                parameterWithName("size").description("페이지 크기")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.content[].inquiryId").type(JsonFieldType.NUMBER).description("문의 ID"),
                                fieldWithPath("data.content[].memberId").type(JsonFieldType.NUMBER).description("작성자 ID"),
                                fieldWithPath("data.content[].type").type(JsonFieldType.STRING).description("문의 타입"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("문의 제목"),
                                fieldWithPath("data.content[].content").type(JsonFieldType.STRING).description("문의 내용"),
                                fieldWithPath("data.content[].processed").type(JsonFieldType.BOOLEAN).description("처리 여부"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("작성 시각 (ISO-8601)"),
                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("총 검색 결과"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열" )
                        )
                ));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/inquiries/{id}/answer - 관리자 답변 등록")
    @WithMockUser(roles = "ADMIN")
    void answerInquiryForAdmin() throws Exception {
        // GIVEN
        Long inquiryId = 42L;
        var form = new InquiryAnswerForm("회신 내용");
        doNothing().when(inquiryService).answerInquiry(inquiryId, form);

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/admin/inquiries/{id}/answer", inquiryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("admin-answer-inquiry",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("문의 ID")
                        ),
                        requestFields(
                                fieldWithPath("answer")
                                        .type(JsonFieldType.STRING)
                                        .description("관리자가 남기는 답변 내용")
                        ),
                        responseFields(
                                fieldWithPath("success")
                                        .type(JsonFieldType.BOOLEAN)
                                        .description("성공 여부"),
                                fieldWithPath("data")
                                        .type(JsonFieldType.NULL)
                                        .description("항상 null"),
                                fieldWithPath("message")
                                        .type(JsonFieldType.STRING)
                                        .description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }

    @Test
    @DisplayName("PUT /admin/inquiries/{id}/processed - 관리자 처리 API")
    @WithMockUser(roles = "ADMIN")
    void markProcessed() throws Exception {
        mockMvc.perform(put("/admin/inquiries/{id}/processed", 99L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andDo(document("mark-processed",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("문의 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지 또는 빈 문자열")
                        )
                ));
    }
}
