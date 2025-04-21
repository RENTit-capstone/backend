package com.capstone.rentit.file.service;

import com.capstone.rentit.file.FileStorageException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    private LocalFileStorageService service;
    private List<Path> toDelete;

    // 강제 저장 경로
    private static final Path FORCED_UPLOAD_PATH =
            Paths.get("C:\\Users\\ADMIN\\Documents").toAbsolutePath().normalize();

    @BeforeEach
    void setUp() {
        // 서비스 인스턴스 생성 및 초기화
        service = new LocalFileStorageService();
        service.init();

        // 테스트 중 생성된 파일 경로를 추적
        toDelete = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // 테스트 중 생성된 파일만 삭제
        toDelete.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {}
        });
    }

    @Test
    @DisplayName("store() 호출 시 파일이 강제 경로에 저장되고 반환된 경로에 파일이 존재해야 한다")
    void testStoreFile() throws Exception {
        // given: Mock MultipartFile
        byte[] content = "테스트 이미지 데이터".getBytes();
        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "my-image.png",
                "image/png",
                content
        );

        // when: store 호출
        String returnedPath = service.store(multipart);

        // then: 반환 경로가 강제 저장 경로로 시작
        assertTrue(returnedPath.startsWith(FORCED_UPLOAD_PATH.toString()),
                "반환된 경로는 강제 업로드 디렉토리에서 시작해야 합니다.");

        // then: 실제 저장된 파일 확인
        Path saved = Paths.get(returnedPath);
        toDelete.add(saved);

        assertTrue(Files.exists(saved), "디스크에 파일이 저장되어야 합니다.");
        assertArrayEquals(content, Files.readAllBytes(saved),
                "저장된 파일 내용이 원본과 동일해야 합니다.");

        // 파일 위치 로그 출력
        System.out.println("✅ 파일 저장 위치: " + saved.toAbsolutePath());
    }

    @Test
    @DisplayName("store() 호출 시 파일명 생성 규칙(확장자 유지, UUID 사용)을 지켜야 한다")
    void testGeneratedFilenamePattern() throws Exception {
        // given: 여러 번 저장하면서 파일명 패턴 확인
        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "example.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        List<String> filenames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String path = service.store(multipart);
            Path saved = Paths.get(path);
            toDelete.add(saved);
            String name = saved.getFileName().toString();
            filenames.add(name);

            // UUID 형식 + .jpg 로 끝나는지 검증
            assertTrue(name.matches("[0-9a-fA-F-]{36}\\.jpg"),
                    "파일명은 UUID 형식이어야 하며 .jpg 확장자를 가져야 합니다.");
        }

        // 생성된 파일명이 모두 유일해야 함
        long distinctCount = filenames.stream().distinct().count();
        assertEquals(3, distinctCount,
                "여러 번 저장 시 파일명이 중복되지 않아야 합니다.");
    }

    @Test
    @DisplayName("store() 호출 시 파일이 없거나 IO 오류 시 FileStorageException을 던진다")
    void testStoreFileThrowsOnError() {
        // given: 잘못된 MultipartFile 구현 (InputStream 오류 유발)
        MockMultipartFile broken = new MockMultipartFile(
                "file",
                "broken.png",
                "image/png",
                new byte[0]
        ) {
            @Override
            public java.io.InputStream getInputStream() throws java.io.IOException {
                throw new java.io.IOException("Stream error");
            }
        };

        // when / then: 예외 발생
        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> service.store(broken)
        );
        assertTrue(ex.getMessage().contains("로컬 파일 저장에 실패했습니다"));
    }
}