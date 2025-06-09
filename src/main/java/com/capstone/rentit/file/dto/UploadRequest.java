package com.capstone.rentit.file.dto;

import lombok.Data;

@Data
public class UploadRequest {
    private String fileName;
    private String contentType;
}
