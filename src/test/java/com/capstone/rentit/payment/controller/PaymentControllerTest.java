package com.capstone.rentit.payment.controller;

import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.payment.dto.*;
import com.capstone.rentit.payment.service.PaymentService;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.controller.RentalController;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean PaymentService paymentService;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean MemberDetailsService memberDetailsService;
    @MockitoBean FileStorageService fileStorageService;

    private static final long MEMBER_ID = 100L;
    private static final long AMOUNT    = 10_000L;

    @Test @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/wallet 계좌 등록 성공")
    void register_success() throws Exception {
        AccountRegisterRequest req =
                new AccountRegisterRequest(MEMBER_ID, "155566677788899900001111", "011");
        given(paymentService.registerAccount(any())).willReturn(MEMBER_ID);

        mockMvc.perform(post("/api/v1/wallet")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(MEMBER_ID))
                .andDo(document("wallet-register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("멤버 ID"),
                                fieldWithPath("finAcno").type(JsonFieldType.STRING).description("핀어카운트"),
                                fieldWithPath("bankCode").type(JsonFieldType.STRING).description("은행 코드")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("등록된 지갑 소유자 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @Test @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/wallet 계좌 조회 성공")
    void getAccount_success() throws Exception {
        WalletResponse res = new WalletResponse(
                MEMBER_ID, AMOUNT, "1555666777...", "011", null, null);

        Student student = Student.builder().memberId(MEMBER_ID).role(MemberRoleEnum.STUDENT).build();
        MemberDetails md = new MemberDetails(student);
        Authentication auth = new UsernamePasswordAuthenticationToken(md, null, md.getAuthorities());

        given(paymentService.getAccount(MEMBER_ID)).willReturn(res);

        mockMvc.perform(get("/api/v1/wallet")
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.finAcno").value(res.finAcno()))
                .andDo(document("wallet-get-account",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("API 호출 성공 여부"),
                                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER)
                                        .description("지갑 소유자 ID"),
                                fieldWithPath("data.balance").type(JsonFieldType.NUMBER)
                                        .description("현재 포인트 잔액"),
                                fieldWithPath("data.finAcno").type(JsonFieldType.STRING)
                                        .description("핀-어카운트(계좌 토큰)"),
                                fieldWithPath("data.bankCode").type(JsonFieldType.STRING)
                                        .description("은행 코드"),
                                fieldWithPath("data.consentAt").type(JsonFieldType.NULL)
                                        .description("연동 동의 시각(없으면 null)"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.NULL)
                                        .description("동의 만료 시각(없으면 null)"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("성공 시 빈 문자열")
                        )
                ));
    }

    @Test @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/wallet/top-up 지갑 충전 성공")
    void topUp_success() throws Exception {
        given(paymentService.topUp(any())).willReturn(1L);

        mockMvc.perform(post("/api/v1/wallet/top-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new TopUpRequest(MEMBER_ID, AMOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("wallet-top-up",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                fieldWithPath("amount").type(JsonFieldType.NUMBER).description("충전 금액(원)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("결제 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @Test @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/wallet/withdraw 지갑 인출 성공")
    void withdraw_success() throws Exception {
        given(paymentService.withdraw(any())).willReturn(2L);

        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new WithdrawRequest(MEMBER_ID, AMOUNT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("wallet-withdraw",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                fieldWithPath("amount").type(JsonFieldType.NUMBER).description("출금 금액(원)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.NUMBER).description("결제 ID"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }

    @Test @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/payments 사용자 결제내역 조회")
    void getPayments_user() throws Exception {
        PaymentResponse sample = new PaymentResponse(
                2L, PaymentType.RENTAL_FEE, PaymentStatus.APPROVED, 5_000L, LocalDateTime.now(),
                "item_name", "소유자이름", "대여자 이름");
        given(paymentService.getPayments(any())).willReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/payments")
                        .queryParam("memberId", String.valueOf(MEMBER_ID))
                        .queryParam("type", "TOP_UP")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("payments-user",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("memberId").description("조회 대상 멤버 ID"),
                                parameterWithName("type").optional()
                                        .description("필터링할 결제 타입 (없으면 전체)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data[].paymentId").type(JsonFieldType.NUMBER).description("결제 ID"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING).description("결제 종류"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("결제 상태"),
                                fieldWithPath("data[].amount").type(JsonFieldType.NUMBER).description("금액"),
                                fieldWithPath("data[].itemName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 물품 이름"),
                                fieldWithPath("data[].ownerName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 소유자 이름"),
                                fieldWithPath("data[].renterName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 대여자 이름"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("결제 시각"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));

    }

    @Test @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/payments 관리자 결제내역 조회")
    void getPayments_admin() throws Exception {
        PaymentResponse sample = new PaymentResponse(
                2L, PaymentType.RENTAL_FEE, PaymentStatus.APPROVED, 5_000L, LocalDateTime.now(),
                "item_name", "소유자이름", "대여자 이름");
        given(paymentService.getPayments(any())).willReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .queryParam("type", "WITHDRAWAL")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("payments-admin",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("memberId").optional()
                                        .description("(선택) 특정 사용자만 조회"),
                                parameterWithName("type").optional()
                                        .description("필터링할 결제 타입")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("API 호출 성공 여부"),
                                fieldWithPath("data[].paymentId").type(JsonFieldType.NUMBER).description("결제 ID"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING).description("결제 종류"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("결제 상태"),
                                fieldWithPath("data[].amount").type(JsonFieldType.NUMBER).description("금액"),
                                fieldWithPath("data[].itemName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 물품 이름"),
                                fieldWithPath("data[].ownerName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 소유자 이름"),
                                fieldWithPath("data[].renterName").type(JsonFieldType.STRING).description("RENTAL_FEE type일 때, 대여자 이름"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("결제 시각"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("성공 시 빈 문자열")
                        )
                ));
    }
}
