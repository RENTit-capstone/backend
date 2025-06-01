package com.capstone.rentit.file.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.file.dto.UploadPresignedResponse;
import com.capstone.rentit.file.dto.UploadRequest;
import com.capstone.rentit.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileStorageService storageService;

    @PostMapping("/upload")
    public CommonResponse<String> upload(@RequestPart("file") MultipartFile file) {
        log.info("이미지 업로드 시작");
        String key = storageService.store(file);
        String presignedUrl = storageService.generatePresignedUrl(key);
        return CommonResponse.success(key);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/presigned/upload")
    public CommonResponse<UploadPresignedResponse> getUploadUrl(
            @RequestBody UploadRequest form) {

        UploadPresignedResponse res = storageService.generateUploadPresignedUrl(form.getFileName(), form.getContentType());
        return CommonResponse.success(res);
    }
}