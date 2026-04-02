package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.FileEntity;

import java.time.LocalDateTime;

public class FileResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;

    public static FileResponse from(FileEntity file) {
        FileResponse response = new FileResponse();
        response.id = file.getId();
        response.originalFilename = file.getOriginalFilename();
        response.contentType = file.getContentType();
        response.size = file.getFileSize();
        response.uploadedAt = file.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}