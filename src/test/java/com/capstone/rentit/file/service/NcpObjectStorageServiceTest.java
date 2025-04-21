package com.capstone.rentit.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NcpObjectStorageService 단위 테스트")
class NcpObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private NcpObjectStorageService service;

    @Captor
    private ArgumentCaptor<PutObjectRequest> putReqCaptor;

    @Captor
    private ArgumentCaptor<RequestBody> bodyCaptor;

    @Captor
    private ArgumentCaptor<GetObjectPresignRequest> presignReqCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(service, "presignExpireMinutes", 15L);
    }

    @Test
    @DisplayName("store(): 확장자 있는 파일 → .png 붙은 UUID 키 생성, S3Client.putObject 호출")
    void store_withExtension() throws Exception {
        // given
        byte[] content = "hello".getBytes();
        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("foo.png");
        given(file.getContentType()).willReturn("image/png");
        given(file.getSize()).willReturn((long) content.length);
        given(file.getInputStream()).willReturn(new ByteArrayInputStream(content));

        // when
        String key = service.store(file);

        // then
        assertThat(key).matches("[0-9a-fA-F\\-]{36}\\.png");
        then(s3Client).should().putObject(putReqCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest putReq = putReqCaptor.getValue();
        RequestBody body = bodyCaptor.getValue();

        assertThat(putReq.bucket()).isEqualTo("test-bucket");
        assertThat(putReq.key()).isEqualTo(key);
        assertThat(putReq.contentType()).isEqualTo("image/png");
        assertThat(body.contentStreamProvider().newStream().readAllBytes())
                .containsExactly(content);
    }

    @Test
    @DisplayName("store(): 확장자 없는 파일 → UUID 키 생성")
    void store_withoutExtension() throws Exception {
        byte[] content = "data".getBytes();
        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("nofile");
        given(file.getContentType()).willReturn("application/octet-stream");
        given(file.getSize()).willReturn((long) content.length);
        given(file.getInputStream()).willReturn(new ByteArrayInputStream(content));

        String key = service.store(file);

        assertThat(key).matches("[0-9a-fA-F\\-]{36}");
        then(s3Client).should().putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("store(): InputStream 실패 시 RuntimeException")
    void store_ioException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("err.txt");
        given(file.getContentType()).willReturn("text/plain");
        given(file.getInputStream()).willThrow(new IOException("fail"));

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 업로드 실패");

        then(s3Client).should(never()).putObject((PutObjectRequest) any(), (RequestBody) any());
    }

    @Test
    @DisplayName("generatePresignedUrl(): presigner 호출 및 URL 반환")
    void generatePresignedUrl_success() throws MalformedURLException {
        // given
        String objectKey = "my-object";
        PresignedGetObjectRequest presignedMock = mock(PresignedGetObjectRequest.class);
        given(presignedMock.url()).willReturn(URI.create("https://example.com/" + objectKey + "?sig=123").toURL());
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .willReturn(presignedMock);

        // when
        String url = service.generatePresignedUrl(objectKey);

        // then
        assertThat(url).isEqualTo("https://example.com/" + objectKey + "?sig=123");
        then(s3Presigner).should().presignGetObject(presignReqCaptor.capture());

        GetObjectPresignRequest captured = presignReqCaptor.getValue();
        assertThat(captured.getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captured.getObjectRequest().key()).isEqualTo(objectKey);
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(15));
    }
}