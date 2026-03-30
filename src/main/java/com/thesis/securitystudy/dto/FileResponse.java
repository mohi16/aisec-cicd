package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.FileEntity;

public class FileResponse {

    private Long id;
    private String originalFilename;
    private String contentType;
    private long fileSize;

    public FileResponse(FileEntity fileEntity) {
        this.id = fileEntity.getId();
        this.originalFilename = fileEntity.getOriginalFilename();
        this.contentType = fileEntity.getContentType();
        this.fileSize = fileEntity.getFileSize();
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
