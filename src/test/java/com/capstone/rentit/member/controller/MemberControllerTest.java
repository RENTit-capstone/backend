package com.capstone.rentit.member.controller;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.dto.ItemBriefResponse;
import com.capstone.rentit.login.filter.JwtAuthenticationFilter;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.MyProfileResponse;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.dto.StudentUpdateForm;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.rental.dto.RentalBriefResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class MemberControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    @DisplayName("POST /api/v1/admin/members → 생성 후 ID 반환")
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
        form.setGender(GenderEnum.MEN);
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
        given(memberService.getMemberById(generatedId))
                .willReturn(MemberDto.fromEntity(student, ""));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/admin/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)));

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
    @DisplayName("GET /api/v1/admin/members → 전체 회원 목록 반환")
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
                .gender(GenderEnum.MEN)
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        given(memberService.getAllMembers())
                .willReturn(Collections.singletonList(MemberDto.fromEntity(student, "")));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/admin/members")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].memberId").value(id))
                .andDo(document("get-all-members",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data[].memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
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
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/members/{id} → 단일 회원 조회")
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
                .gender(GenderEnum.MEN)
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();
        given(memberService.getMemberById(id))
                .willReturn(MemberDto.fromEntity(student, ""));

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/members/{id}", id)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(id))
                .andDo(document("get-member",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
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
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/v1/members → 로그인 회원 정보 업데이트")
    @Test
    void updateMember_success() throws Exception {
        // given
        long id = 1L;
        StudentUpdateForm form = new StudentUpdateForm();
        form.setName("Updated Student");
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
                .gender(GenderEnum.MEN)
                .role(MemberRoleEnum.STUDENT)
                .locked(false)
                .createdAt(LocalDate.now())
                .build();

        MemberDetails details = new MemberDetails(updated);
        Authentication auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        doNothing().when(memberService)
                .updateMember(eq(id), any(StudentUpdateForm.class), any(MultipartFile.class));

        MockMultipartFile jsonPart = new MockMultipartFile(
                "form", "", "application/json",
                objectMapper.writeValueAsBytes(form));
        MockMultipartFile image = new MockMultipartFile(
                "image", "a.jpg", "image/jpeg", "dummy".getBytes());
        // when
        ResultActions result = mockMvc.perform(multipart("/api/v1/members", id)
                .file(jsonPart).file(image)
                .with(request -> { request.setMethod("PUT"); return request; })
                .with(csrf()).with(authentication(auth)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("update-member",
                        requestParts(
                                partWithName("form").description("MemberUpdateForm JSON"),
                                partWithName("image").optional().description("교체 이미지 파일 (1개)")
                        ),
                        requestPartFields("form",
                                fieldWithPath("name").type(JsonFieldType.STRING).description("변경할 이름"),
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
    @DisplayName("DELETE /api/v1/admin/members/{id} → 회원 삭제")
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
    @DisplayName("GET /api/v1/members/me → 로그인 회원 반환")
    @Test
    void getLoginMember_success() throws Exception {
        // given
        Long memberId = 99L;
        Student student = Student.builder()
                .memberId(memberId).email("login@student.com")
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

        MyProfileResponse fakeProfile = MyProfileResponse.builder()
                .memberId(memberId)
                .email("login@student.com")
                .name("Login Student")
                .profileImg("profile_img_url")
                .items(List.of(
                        ItemBriefResponse.builder()
                                .itemId(10L).name("item1")
                                .build()
                ))
                .ownedRentals(List.of(
                        RentalBriefResponse.builder()
                                .rentalId(100L).itemName("item1")
                                .requestDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(3))
                                .build()
                ))
                .rentedRentals(List.of(
                        RentalBriefResponse.builder()
                                .rentalId(101L).itemName("item2")
                                .requestDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(3))
                                .build()
                ))
                .build();

        given(memberService.getMyProfile(memberId))
                .willReturn(fakeProfile);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/members/me")
                .with(user(details))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(99))
                .andExpect(jsonPath("$.data.email").value("login@student.com"))
                .andExpect(jsonPath("$.data.name").value("Login Student"))
                .andDo(document("get-login-member",
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data.role").type(JsonFieldType.NULL).description("역할 (현재 null)"),
                                fieldWithPath("data.profileImg").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.NULL).description("가입일자 (현재 null)"),

                                subsectionWithPath("data.items").type(JsonFieldType.ARRAY).description("등록한 아이템 목록"),
                                fieldWithPath("data.items[].itemId").type(JsonFieldType.NUMBER).description("아이템 ID"),
                                fieldWithPath("data.items[].name").type(JsonFieldType.STRING).description("아이템 이름"),
                                fieldWithPath("data.items[].price").type(JsonFieldType.NULL).description("가격 (현재 null)"),
                                fieldWithPath("data.items[].status").type(JsonFieldType.NULL).description("상태 (현재 null)"),
                                fieldWithPath("data.items[].thumbnailUrl").type(JsonFieldType.NULL).description("썸네일 URL (현재 null)"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.NULL).description("등록일자 (현재 null)"),

                                subsectionWithPath("data.ownedRentals").type(JsonFieldType.ARRAY).description("소유자로서 대여된 목록"),
                                fieldWithPath("data.ownedRentals[].rentalId").type(JsonFieldType.NUMBER).description("대여 거래 ID"),
                                fieldWithPath("data.ownedRentals[].itemName").type(JsonFieldType.STRING).description("아이템 이름"),
                                fieldWithPath("data.ownedRentals[].ownerName").type(JsonFieldType.NULL).description("소유자 이름 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].renterName").type(JsonFieldType.NULL).description("대여자 이름 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].status").type(JsonFieldType.NULL).description("대여 상태 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].requestDate").type(JsonFieldType.STRING).description("대여 요청일시"),
                                fieldWithPath("data.ownedRentals[].startDate").type(JsonFieldType.NULL).description("대여 시작일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].dueDate").type(JsonFieldType.STRING).description("반납 예정일시"),
                                fieldWithPath("data.ownedRentals[].approvedDate").type(JsonFieldType.NULL).description("승인일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].rejectedDate").type(JsonFieldType.NULL).description("거부일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].leftAt").type(JsonFieldType.NULL).description("락커 입고일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].pickedUpAt").type(JsonFieldType.NULL).description("수령일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].returnedAt").type(JsonFieldType.NULL).description("반납일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].retrievedAt").type(JsonFieldType.NULL).description("락커 인출일시 (현재 null)"),
                                fieldWithPath("data.ownedRentals[].thumbnailUrl").type(JsonFieldType.NULL).description("썸네일 URL (현재 null)"),
                                fieldWithPath("data.ownedRentals[].owner").type(JsonFieldType.BOOLEAN).description("내가 소유자인지 여부"),

                                subsectionWithPath("data.rentedRentals").type(JsonFieldType.ARRAY).description("대여자로서 빌린 목록"),
                                fieldWithPath("data.rentedRentals[].rentalId").type(JsonFieldType.NUMBER).description("대여 거래 ID"),
                                fieldWithPath("data.rentedRentals[].itemName").type(JsonFieldType.STRING).description("아이템 이름"),
                                fieldWithPath("data.rentedRentals[].ownerName").type(JsonFieldType.NULL).description("소유자 이름 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].renterName").type(JsonFieldType.NULL).description("대여자 이름 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].status").type(JsonFieldType.NULL).description("대여 상태 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].requestDate").type(JsonFieldType.STRING).description("대여 요청일시"),
                                fieldWithPath("data.rentedRentals[].startDate").type(JsonFieldType.NULL).description("대여 시작일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].dueDate").type(JsonFieldType.STRING).description("반납 예정일시"),
                                fieldWithPath("data.rentedRentals[].approvedDate").type(JsonFieldType.NULL).description("승인일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].rejectedDate").type(JsonFieldType.NULL).description("거부일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].leftAt").type(JsonFieldType.NULL).description("락커 입고일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].pickedUpAt").type(JsonFieldType.NULL).description("수령일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].returnedAt").type(JsonFieldType.NULL).description("반납일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].retrievedAt").type(JsonFieldType.NULL).description("락커 인출일시 (현재 null)"),
                                fieldWithPath("data.rentedRentals[].thumbnailUrl").type(JsonFieldType.NULL).description("썸네일 URL (현재 null)"),
                                fieldWithPath("data.rentedRentals[].owner").type(JsonFieldType.BOOLEAN).description("내가 소유자인지 여부"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공시 빈 문자열")
                        )
                ));
    }
}