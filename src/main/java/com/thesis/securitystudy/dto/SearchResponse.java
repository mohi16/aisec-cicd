package com.thesis.securitystudy.dto;

public class SearchResponse {
    private String title;
    private String content;
    private boolean isPublic;

    public SearchResponse(String title, String content, boolean isPublic) {
        this.title = title;
        this.content = content;
        this.isPublic = isPublic;
    }

    // Getters and setters
}
