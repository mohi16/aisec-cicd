package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Note;

import java.time.LocalDateTime;

public class SearchResponse {

    private Long id;
    private String title;
    private String snippet;
    private String ownerUsername;
    private boolean isPublic;
    private boolean isEncrypted;
    private LocalDateTime createdAt;

    public static SearchResponse from(Note note) {
        SearchResponse r = new SearchResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        // Use full content as snippet for now; controller/service could shorten later
        r.snippet = note.getContent();
        r.ownerUsername = note.getOwner().getUsername();
        r.isPublic = note.isPublic();
        r.isEncrypted = note.isEncrypted();
        r.createdAt = note.getCreatedAt();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSnippet() { return snippet; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isPublic() { return isPublic; }
    public boolean isEncrypted() { return isEncrypted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

