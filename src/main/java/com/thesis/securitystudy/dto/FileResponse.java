package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.FileEntity;

import java.time.LocalDateTime;

public class FileResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;

    public static FileResponse from(FileEntity fe) {
        FileResponse r = new FileResponse();
        r.id = fe.getId();
        r.originalFilename = fe.getOriginalFilename();
        r.contentType = fe.getContentType();
        r.size = fe.getFileSize();
        r.uploadedAt = fe.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}