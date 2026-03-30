package com.thesis.securitystudy.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
