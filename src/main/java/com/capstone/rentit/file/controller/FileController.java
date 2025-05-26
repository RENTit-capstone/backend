package com.capstone.rentit.file.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
}