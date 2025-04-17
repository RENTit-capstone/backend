package com.capstone.rentit.member.controller;

import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.dto.StudentUpdateForm;
import com.capstone.rentit.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(WebConfig.class)  // 커스텀 리졸버가 등록된 설정을 포함시킵니다.
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    void createMember_success() throws Exception {
        // 요청: 학생 회원 생성 DTO (StudentCreateForm)
        StudentCreateForm createForm = new StudentCreateForm();
        createForm.setEmail("student@example.com");
        createForm.setPassword("password");
        createForm.setName("Test Student");
        createForm.setNickname("studentNick");
        createForm.setPhone("010-1234-5678");
        createForm.setUniversity("Test University");
        createForm.setStudentId("S12345678");
        createForm.setGender("M");
        createForm.setProfileImg(null);

        // Mock 처리
        Long generatedId = 1L;
        when(memberService.createMember(any(StudentCreateForm.class))).thenReturn(generatedId);
        // 실제 도메인 객체 생성 (테스트용 Student)
        Student student = Student.builder()
                .memberId(generatedId)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)  // 반드시 추가
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        when(memberService.getUser(generatedId)).thenReturn(Optional.of(student));

        String json = objectMapper.writeValueAsString(createForm);

        mockMvc.perform(post("/api/v1/admin/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(generatedId))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("admin-create-member-success",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("사용자 비밀번호").type(JsonFieldType.STRING),
                                fieldWithPath("name").description("사용자 이름").type(JsonFieldType.STRING),
                                fieldWithPath("nickname").description("사용자 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("phone").description("연락처").type(JsonFieldType.STRING),
                                fieldWithPath("memberType").description("회원 역할").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("gender").description("성별").type(JsonFieldType.STRING),
                                fieldWithPath("profileImg").description("프로필 이미지 URL (선택)").optional().type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("생성된 회원의 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").description("성공시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));
    }

    @Test
    void getAllMembers_success() throws Exception {
        // 단일 회원을 리스트로 반환
        Long id = 1L;
        Student student = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)  // 반드시 추가
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        when(memberService.getAllUsers()).thenReturn(Collections.singletonList(student));

        mockMvc.perform(get("/api/v1/admin/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andDo(document("get-all-members",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data[].id").description("생성된 회원의 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data[].email").description("회원 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("data[].name").description("회원 이름").type(JsonFieldType.STRING),
                                fieldWithPath("data[].role").description("회원 역할").type(JsonFieldType.STRING),
                                fieldWithPath("data[].profileImg").description("프로필 이미지 URL (선택)").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data[].createdAt").description("회원 등록일").type(JsonFieldType.STRING),
                                fieldWithPath("data[].locked").description("계정 잠금 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data[].nickname").description("학생 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("data[].gender").description("학생 성별").type(JsonFieldType.STRING),
                                fieldWithPath("data[].studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("data[].university").description("학생 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("data[].phone").description("학생 연락처").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));
    }

    @Test
    void getMember_success() throws Exception {
        Long id = 1L;
        Student student = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)  // 반드시 추가
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        when(memberService.getUser(id)).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/v1/members/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andDo(document("get-member",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.id").description("생성된 회원의 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.email").description("회원 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("data.name").description("회원 이름").type(JsonFieldType.STRING),
                                fieldWithPath("data.role").description("회원 역할").type(JsonFieldType.STRING),
                                fieldWithPath("data.profileImg").description("프로필 이미지 URL (선택)").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.createdAt").description("회원 등록일").type(JsonFieldType.STRING),
                                fieldWithPath("data.locked").description("계정 잠금 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.nickname").description("학생 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("data.gender").description("학생 성별").type(JsonFieldType.STRING),
                                fieldWithPath("data.studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("data.university").description("학생 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("data.phone").description("학생 연락처").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("성공시 빈 문자열").type(JsonFieldType.STRING)
                        )
                ));
    }

    @Test
    void updateMember_success() throws Exception {
        Long id = 1L;
        StudentUpdateForm updateForm = new StudentUpdateForm();
        updateForm.setName("Updated Student");
        updateForm.setProfileImg("updated.jpg");
        updateForm.setNickname("updatedNick");
        updateForm.setPhone("010-9876-5432");

        Student updatedStudent = Student.builder()
                .memberId(id)
                .email("student@example.com")
                .name("Test Student")
                .nickname("studentNick")
                .phone("010-1234-5678")
                .university("Test University")
                .studentId("S12345678")
                .gender("M")
                .role(MemberRoleEnum.STUDENT)  // 반드시 추가
                .locked(false)
                .createdAt(LocalDate.now())
                .build();

        when(memberService.updateUser(eq(id), any(StudentUpdateForm.class)))
                .thenReturn(updatedStudent);

        String json = objectMapper.writeValueAsString(updateForm);

        mockMvc.perform(put("/api/v1/members/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(id))
                .andDo(document("update-member",
                        requestFields(
                                fieldWithPath("name").description("업데이트할 회원 이름").type(JsonFieldType.STRING),
                                fieldWithPath("profileImg").description("업데이트할 프로필 이미지 URL").type(JsonFieldType.STRING),
                                fieldWithPath("nickname").description("업데이트할 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("phone").description("업데이트할 연락처").type(JsonFieldType.STRING),
                                fieldWithPath("memberType").description("회원 역할").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("수정된 회원의 ID, 실패 시 null").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @Test
    void deleteMember_success() throws Exception {
        Long id = 1L;
        // deleteMember 엔드포인트는 void를 반환하므로, 상태 200 OK만 검증함
        mockMvc.perform(delete("/api/v1/admin/members/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("delete-member",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("삭제된 회원의 ID, 실패 시 null").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    @Test
    void getLoginMember_success() throws Exception {
        // 도메인 객체(Student) 생성
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

        // MemberDetails 객체로 감싸기
        MemberDetails memberDetails = new MemberDetails(student);

        // SecurityContext에 MemberDetails를 principal로 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberDetails, null)
        );

        mockMvc.perform(get("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.email").value("login@student.com"))
                .andExpect(jsonPath("$.data.name").value("Login Student"))
                .andDo(document("get-login-member",
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.id").description("로그인된 회원의 ID").type(JsonFieldType.NUMBER),
                                fieldWithPath("data.email").description("로그인된 회원 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("data.name").description("로그인된 회원 이름").type(JsonFieldType.STRING),
                                fieldWithPath("data.role").description("로그인된 회원 역할").type(JsonFieldType.STRING),
                                fieldWithPath("data.profileImg").description("프로필 이미지 URL (선택)").optional().type(JsonFieldType.STRING),
                                fieldWithPath("data.createdAt").description("회원 등록일").type(JsonFieldType.STRING),
                                fieldWithPath("data.locked").description("계정 잠금 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data.nickname").description("회원 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("data.gender").description("회원 성별").type(JsonFieldType.STRING),
                                fieldWithPath("data.studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("data.university").description("학생 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("data.phone").description("회원 연락처").type(JsonFieldType.STRING),
                                fieldWithPath("message").description("응답 메시지 (성공 시 빈 문자열)").type(JsonFieldType.STRING)
                        )
                ));
    }
}