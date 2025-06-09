package com.capstone.rentit.file.service;

import com.capstone.rentit.file.dto.UploadPresignedResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);
    String generatePresignedUrl(String objectKey);
    UploadPresignedResponse generateUploadPresignedUrl(String originalFilename, String contentType);

}
