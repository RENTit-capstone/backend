package com.capstone.rentit.register.controller;

import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.register.dto.StudentRegisterForm;
import com.capstone.rentit.register.service.UnivCertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnivCertService univCertService;

    @MockitoBean
    private MemberService memberService;

    /**
     * 회원 가입 성공 케이스 테스트
     */
    @Test
    public void register_member_success() throws Exception {
        // 요청 데이터 준비
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setRole(1);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        // 이미 등록된 이메일이 아님을 가정
        when(memberService.findByEmail("test@example.com")).thenReturn(Optional.empty());
        // 인증된 이메일임을 가정
        when(univCertService.isCertified("test@example.com")).thenReturn(true);
        // 학생 생성 시 1번 ID가 리턴되는 것을 모킹
        Long generatedId = 1L;
        when(memberService.createStudent(any(StudentRegisterForm.class))).thenReturn(generatedId);

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(generatedId))
                .andExpect(jsonPath("$.message").value(""))
                .andDo(document("register-member-success",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("사용자 비밀번호").type(JsonFieldType.STRING),
                                fieldWithPath("name").description("사용자 이름").type(JsonFieldType.STRING),
                                fieldWithPath("role").description("사용자 역할 (예: 1은 학생)").type(JsonFieldType.NUMBER),
                                fieldWithPath("nickname").description("사용자 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("사용자 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("gender").description("성별").type(JsonFieldType.STRING),
                                fieldWithPath("phone").description("연락처").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("생성된 회원의 ID, 실패 시 null").type(JsonFieldType.NUMBER),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("빈 문자열")
                        )
                ));
    }

    /**
     * 이미 등록된 이메일인 경우의 회원 가입 실패 테스트
     */
    @Test
    public void register_member_email_already_exists() throws Exception {
        // 요청 데이터 준비
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setRole(1);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        // 이미 등록된 이메일로 회원정보가 존재한다고 가정
        Student existingStudent = Student.builder()
                .name(form.getName())
                .email(form.getEmail())
                .password("encodedPassword")
                .nickname(form.getNickname())
                .studentId(form.getStudentId())
                .university(form.getUniversity())
                .gender(form.getGender())
                .phone(form.getPhone())
                .createdAt(LocalDate.now())
                .locked(false)
                .build();

        when(memberService.findByEmail("test@example.com")).thenReturn(Optional.of(existingStudent));

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("이미 등록된 이메일입니다."))
                .andDo(document("register-member-email-exists",
                        requestFields(
                                fieldWithPath("email").description("사용자 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("password").description("사용자 비밀번호").type(JsonFieldType.STRING),
                                fieldWithPath("name").description("사용자 이름").type(JsonFieldType.STRING),
                                fieldWithPath("role").description("사용자 역할 (예: 1은 학생)").type(JsonFieldType.NUMBER),
                                fieldWithPath("nickname").description("사용자 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("university").description("사용자 소속 대학").type(JsonFieldType.STRING),
                                fieldWithPath("studentId").description("학생 학번").type(JsonFieldType.STRING),
                                fieldWithPath("gender").description("성별").type(JsonFieldType.STRING),
                                fieldWithPath("phone").description("연락처").type(JsonFieldType.STRING)
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부").type(JsonFieldType.BOOLEAN),
                                fieldWithPath("data").description("실패 시 null 데이터를 반환").optional().type(JsonFieldType.NULL),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                        )
                ));
    }


    /**
     * 인증되지 않은 이메일의 경우의 회원 가입 실패 테스트
     */
    @Test
    public void register_member_email_not_certified() throws Exception {
        // 요청 데이터 준비
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setEmail("test@example.com");
        form.setPassword("password");
        form.setRole(0);
        form.setNickname("tester");
        form.setUniversity("Test University");
        form.setStudentId("12345678");
        form.setGender("M");
        form.setPhone("010-1234-5678");

        // 이메일이 아직 인증되지 않은 상태로 가정
        when(memberService.findByEmail("test@ajou.ac.kr")).thenReturn(Optional.empty());
        when(univCertService.isCertified("test@ajou.ac.kr")).thenReturn(false);

        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("미인증 이메일입니다."))
                .andDo(document("register-member-email-not-certified",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("사용자 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("사용자 비밀번호"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("role").type(JsonFieldType.NUMBER).description("사용자 역할 (예: 0은 학생)"),
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("university").type(JsonFieldType.STRING).description("사용자 소속 대학"),
                                fieldWithPath("studentId").type(JsonFieldType.STRING).description("학생 학번"),
                                fieldWithPath("gender").type(JsonFieldType.STRING).description("성별"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).description("연락처")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                // description을 먼저 지정하고 나서 optional(), type() 순으로 호출
                                fieldWithPath("data").description("실패 시 null 데이터를 반환").optional().type(JsonFieldType.NULL),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                        )
                ));
    }
}