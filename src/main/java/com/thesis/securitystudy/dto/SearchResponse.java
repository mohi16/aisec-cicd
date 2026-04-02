package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Note;

import java.time.LocalDateTime;

public class SearchResponse {
    private Long id;
    private String title;
    private String contentSnippet;
    private String ownerUsername;
    private boolean isPublic;
    private boolean isEncrypted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SearchResponse from(Note note) {
        SearchResponse r = new SearchResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        String content = note.getContent();
        if (content == null) content = "";
        // snippet length 200 chars
        r.contentSnippet = content.length() <= 200 ? content : content.substring(0, 200) + "...";
        r.ownerUsername = note.getOwner() != null ? note.getOwner().getUsername() : null;
        r.isPublic = note.isPublic();
        r.isEncrypted = note.isEncrypted();
        r.createdAt = note.getCreatedAt();
        r.updatedAt = note.getUpdatedAt();
        return r;
    }

    // getters (and setters if needed)
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContentSnippet() { return contentSnippet; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isPublic() { return isPublic; }
    public boolean isEncrypted() { return isEncrypted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
