package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Note;

import java.time.LocalDateTime;

public class SearchResponse {

    private Long id;
    private String title;
    private String contentSnippet;
    private String ownerUsername;
    private boolean isPublic;
    private LocalDateTime createdAt;

    public static SearchResponse from(Note note) {
        SearchResponse response = new SearchResponse();
        response.id = note.getId();
        response.title = note.getTitle();

        String content = note.getContent();
        if (content == null) {
            content = "";
        }

        response.contentSnippet = content.length() <= 200
                ? content
                : content.substring(0, 200) + "...";

        response.ownerUsername = note.getOwner() != null ? note.getOwner().getUsername() : null;
        response.isPublic = note.isPublic();
        response.createdAt = note.getCreatedAt();

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContentSnippet() {
        return contentSnippet;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}