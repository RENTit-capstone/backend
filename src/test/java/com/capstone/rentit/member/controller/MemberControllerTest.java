package com.capstone.rentit.member.controller;

import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.dto.StudentUpdateForm;
import com.capstone.rentit.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class MemberControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    // ↓ 여기를 추가
    @MockitoBean
    private com.capstone.rentit.login.provider.JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private com.capstone.rentit.login.filter.JwtAuthenticationFilter jwtAuthenticationFilter;

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

    @WithMockUser(roles = "ADMIN")
    @DisplayName("학생 회원 생성 성공")
    @Test
    void createMember_success() throws Exception {
        // given
        StudentCreateForm form = new StudentCreateForm();
        form.setEmail("student@example.com");
        form.setPassword("password");
        form.setName("Test Student");
        form.setNickname("studentNick");
        form.setPhone("010-1234-5678");
        form.setUniversity("Test University");
        form.setStudentId("S12345678");
        form.setGender("M");
        form.setProfileImg(null);

        long generatedId = 1L;
        Student student = Student.builder()
                .memberId(generatedId)
                .email(form.getEmail())
                .name(form.getName())
                .nickname(form.getNickname())
                .phone(form.getPhone())
                .university(form.getUniversity())
                .studentId(form.getStudentId())
                .gender(form.getGender())
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();

        given(memberService.createMember(any(StudentCreateForm.class)))
                .willReturn(generatedId);
        given(memberService.getMember(generatedId))
                .willReturn(Optional.of(student));

        String payload = objectMapper.writeValueAsString(form);

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/admin/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(generatedId))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("admin-create-member-success",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("사용자 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).description("연락처"),
                                fieldWithPath("memberType").type(JsonFieldType.STRING).description("회원 역할"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("소속 대학"),
                                fieldWithPath("studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("gender").type(JsonFieldType.STRING).description("성별"),
                                fieldWithPath("profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 URL (선택)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("생성된 회원의 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("모든 회원 조회 성공")
    @Test
    void getAllMembers_success() throws Exception {
        // given
        long id = 1L;
        Student student = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        given(memberService.getAllMembers())
                .willReturn(Collections.singletonList(student));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/admin/members")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andDo(document("get-all-members",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data[].email").type(JsonFieldType.STRING).description("회원 이메일"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("회원 이름"),
                                fieldWithPath("data[].role").type(JsonFieldType.STRING).description("회원 역할"),
                                fieldWithPath("data[].profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("회원 등록일"),
                                fieldWithPath("data[].locked").type(JsonFieldType.BOOLEAN).description("계정 잠금 여부"),
                                fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("학생 닉네임"),
                                fieldWithPath("data[].gender").type(JsonFieldType.STRING).description("학생 성별"),
                                fieldWithPath("data[].studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("data[].university").type(JsonFieldType.STRING).description("학생 소속 대학"),
                                fieldWithPath("data[].phone").type(JsonFieldType.STRING).description("학생 연락처"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("단일 회원 조회 성공")
    @Test
    void getMember_success() throws Exception {
        // given
        long id = 1L;
        Student student = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        given(memberService.getMember(id))
                .willReturn(Optional.of(student));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/members/{id}", id)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andDo(document("get-member",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING).description("회원 이메일"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("회원 이름"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("회원 역할"),
                                fieldWithPath("data.profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("회원 등록일"),
                                fieldWithPath("data.locked").type(JsonFieldType.BOOLEAN).description("계정 잠금 여부"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("학생 닉네임"),
                                fieldWithPath("data.gender").type(JsonFieldType.STRING).description("학생 성별"),
                                fieldWithPath("data.studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("data.university").type(JsonFieldType.STRING).description("학생 소속 대학"),
                                fieldWithPath("data.phone").type(JsonFieldType.STRING).description("학생 연락처"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("회원 정보 수정 성공")
    @Test
    void updateMember_success() throws Exception {
        // given
        long id = 1L;
        StudentUpdateForm form = new StudentUpdateForm();
        form.setName("Updated Student");
        form.setProfileImg("updated.jpg");
        form.setNickname("updatedNick");
        form.setPhone("010-9876-5432");

        Student updated = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name(form.getName())
                .nickname(form.getNickname())
                .phone(form.getPhone())
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();

        MemberDetails details = new MemberDetails(updated);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        given(memberService.updateMember(eq(id), any(StudentUpdateForm.class)))
                .willReturn(updated);

        String payload = objectMapper.writeValueAsString(form);

        // when
        ResultActions result = mockMvc.perform(put("/api/v1/members")
                .with(authentication(auth))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("update-member",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("변경할 이름"),
                                fieldWithPath("profileImg").type(JsonFieldType.STRING).description("변경할 프로필 이미지 URL"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("변경할 닉네임"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).description("변경할 연락처"),
                                fieldWithPath("memberType").type(JsonFieldType.STRING).description("회원 역할")
                                ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "ADMIN")
    @DisplayName("회원 삭제 성공")
    @Test
    void deleteMember_success() throws Exception {
        // given
        long id = 1L;
        // 삭제는 서비스에서 void

        // when
        ResultActions result = mockMvc.perform(delete("/api/v1/admin/members/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andDo(document("delete-member",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("null"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("로그인 회원 조회 성공")
    @Test
    void getLoginMember_success() throws Exception {
        // given
        Student student = Student.builder()
                .memberId(99L)
                .email("login@student.com")
                .name("Login Student")
                .nickname("loginNick")
                .phone("010-1111-1111")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.of(2025, 4, 16))
                .build();

        MemberDetails details = new MemberDetails(student);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/members/me")
                .with(user(details))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.email").value("login@student.com"))
                .andExpect(jsonPath("$.data.name").value("Login Student"))
                .andDo(document("get-login-member",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("로그인 회원 ID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING).description("로그인 회원 이메일"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("로그인 회원 이름"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("로그인 회원 역할"),
                                fieldWithPath("data.profileImg").optional().type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("회원 등록일"),
                                fieldWithPath("data.locked").type(JsonFieldType.BOOLEAN).description("계정 잠금 여부"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("data.gender").type(JsonFieldType.STRING).description("회원 성별"),
                                fieldWithPath("data.studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("data.university").type(JsonFieldType.STRING).description("학생 소속 대학"),
                                fieldWithPath("data.phone").type(JsonFieldType.STRING).description("회원 연락처"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }
}