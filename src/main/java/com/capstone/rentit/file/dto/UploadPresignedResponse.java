package com.capstone.rentit.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UploadPresignedResponse {
    private final String objectKey;
    private final String presignedUrl;
}
