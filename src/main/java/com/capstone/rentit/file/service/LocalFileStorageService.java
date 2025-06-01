package com.capstone.rentit.file.service;

import com.capstone.rentit.file.FileStorageException;
import com.capstone.rentit.file.dto.UploadPresignedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

//@Service("localFileStorageService")
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final Path FORCED_UPLOAD_PATH =
            Paths.get("C:\\Users\\ADMIN\\Documents").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(FORCED_UPLOAD_PATH);
        } catch (IOException e) {
            throw new FileStorageException("로컬 업로드 디렉토리를 생성할 수 없습니다: " + FORCED_UPLOAD_PATH, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) {
            ext = original.substring(idx);
        }

        String filename = UUID.randomUUID().toString() + ext;
        try {
            Path target = FORCED_UPLOAD_PATH.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new FileStorageException("로컬 파일 저장에 실패했습니다: " + original, e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        // 로컬 서비스이므로 presigned URL 개념이 없으면 그냥 파일 경로를 리턴하거나 빈 문자열
        return "";
    }

    @Override
    public UploadPresignedResponse generateUploadPresignedUrl(String originalFilename, String contentType) {
        return null;
    }
}