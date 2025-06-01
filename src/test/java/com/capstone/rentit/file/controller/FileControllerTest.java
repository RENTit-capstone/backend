package com.capstone.rentit.file.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.config.WebConfig;
import com.capstone.rentit.file.dto.UploadPresignedResponse;
import com.capstone.rentit.file.dto.UploadRequest;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import com.capstone.rentit.member.status.MemberRoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(FileController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService storageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private MemberDetailsService memberDetailsService;

    private Authentication authWith(long memberId) {
        var student = com.capstone.rentit.member.domain.Student.builder()
                .memberId(memberId)
                .role(MemberRoleEnum.STUDENT)
                .build();
        return new UsernamePasswordAuthenticationToken(new MemberDetails(student), null, List.of());
    }

    private RestDocumentationResultHandler docsHandler(RestDocumentationContextProvider restDoc) {
        return document("{class-name}/{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
        );
    }

    @WithMockUser(roles = "USER")
    @Test
    @DisplayName("POST /api/v1/files/presigned/upload - presigned upload URL 요청")
    void getUploadPresignedUrl(RestDocumentationContextProvider restDoc) throws Exception {
        // given
        UploadRequest requestDto = new UploadRequest();
        requestDto.setFileName("example.png");
        requestDto.setContentType("image/png");

        UploadPresignedResponse presignedResponse = new UploadPresignedResponse(
                "generated-object-key-123",
                "https://bucket.example.com/generated-object-key-123?X-Amz-Signature=abcd"
        );

        given(storageService.generateUploadPresignedUrl(
                eq(requestDto.getFileName()),
                eq(requestDto.getContentType())
        )).willReturn(presignedResponse);

        String requestJson = """
            {
              "fileName": "example.png",
              "contentType": "image/png"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/v1/files/presigned/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(csrf())
                        .with(authentication(authWith(123L)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectKey").value("generated-object-key-123"))
                .andExpect(jsonPath("$.data.presignedUrl")
                        .value("https://bucket.example.com/generated-object-key-123?X-Amz-Signature=abcd"))
                .andDo(docsHandler(restDoc).document(
                        requestFields(
                                fieldWithPath("fileName").description("업로드할 파일의 원본 파일명"),
                                fieldWithPath("contentType").description("업로드할 파일의 MIME 타입")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 호출 성공 여부"),
                                fieldWithPath("data.objectKey").description("생성된 object key"),
                                fieldWithPath("data.presignedUrl").description("PUT 요청용 Pre-signed URL"),
                                fieldWithPath("message").description("성공시 빈 문자열")
                        )
                ));
    }
}
