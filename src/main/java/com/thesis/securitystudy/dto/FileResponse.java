package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.FileEntity;
import java.time.LocalDateTime;

public class FileResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;

    public static FileResponse from(FileEntity fe) {
        FileResponse r = new FileResponse();
        r.id = fe.getId();
        r.originalFilename = fe.getOriginalFilename();
        r.contentType = fe.getContentType();
        r.fileSize = fe.getFileSize();
        r.createdAt = fe.getCreatedAt();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

