package com.capstone.rentit.notification.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.notification.dto.NotificationDto;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.notification.type.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NotificationService notificationService;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    MemberDetailsService memberDetailsService;
    @MockitoBean
    FileStorageService fileStorageService;

    @Test
    @DisplayName("GET /notifications → 서비스 결과를 CommonResponse로 래핑해 반환한다")
    void listNotifications_returnsWrappedPage() throws Exception {
        //given
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        NotificationDto dto = new NotificationDto(
                42L,
                NotificationType.RENT_REQUESTED,
                "알림 제목",
                "알림 내용",
                false,
                LocalDateTime.now()
        );
        Page<NotificationDto> stubPage = new PageImpl<>(List.of(dto), pageable, 1);

        when(notificationService.findByTarget(any(MemberDto.class), any(Pageable.class)))
                .thenReturn(stubPage);

        when(notificationService.findByTarget(any(MemberDto.class), any(Pageable.class)))
                .thenReturn(stubPage);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc")
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(42))
                .andDo(document("notifications-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        relaxedQueryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("sort").description("정렬 조건, '필드,방향' 형식")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data.content[].id").description("알림 ID"),
                                fieldWithPath("data.content[].type").description("알림 타입"),
                                fieldWithPath("data.content[].title").description("제목"),
                                fieldWithPath("data.content[].body").description("본문"),
                                fieldWithPath("data.content[].read").description("읽음 여부"),
                                fieldWithPath("data.content[].createdAt").description("생성 시각"),
                                fieldWithPath("data.totalElements").description("총 알림 수"),
                                fieldWithPath("data.totalPages").description("총 페이지 수"),
                                fieldWithPath("data.number").description("현재 페이지 번호"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("message").description("실패 시 에러 메시지")
                        )
                ));

        verify(notificationService).findByTarget(any(MemberDto.class), any(Pageable.class));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read → 읽음 처리 후 성공 응답")
    void markAsRead_marksAndReturnsSuccess() throws Exception {
        //given
        Student student = Student.builder().memberId(20L).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());
        long id = 42L;

        doNothing().when(notificationService).markAsRead(eq(id), any(MemberDto.class));

        mockMvc.perform(put("/api/v1/notifications/{id}/read", id)
                .with(csrf())
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("notifications-mark-as-read",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("알림 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data").description("반환 데이터 (항상 null)"),
                                fieldWithPath("message").description("실패 시 에러 메시지")
                        )
                ));

        verify(notificationService).markAsRead(eq(id), any(MemberDto.class));
    }
}
