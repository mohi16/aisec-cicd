// src/main/java/com/thesis/securitystudy/dto/FileMetadataDto.java
package com.thesis.securitystudy.dto;

import java.time.OffsetDateTime;

public class FileMetadataDto {
    private Long id;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private OffsetDateTime createdAt;

    public FileMetadataDto() {}

    public FileMetadataDto(Long id, String originalFilename, String contentType, long fileSize, OffsetDateTime createdAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
