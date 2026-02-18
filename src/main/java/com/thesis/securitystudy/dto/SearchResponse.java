// src/main/java/com/thesis/securitystudy/dto/SearchResponse.java
package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Note;
import java.time.LocalDateTime;

public class SearchResponse {
    private Long id;
    private String title;
    private String content;
    private String ownerUsername;
    private boolean isPublic;
    private boolean isEncrypted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SearchResponse from(Note note) {
        SearchResponse r = new SearchResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        r.content = note.getContent();
        r.ownerUsername = note.getOwner() != null ? note.getOwner().getUsername() : null;
        r.isPublic = note.isPublic();
        r.isEncrypted = note.isEncrypted();
        r.createdAt = note.getCreatedAt();
        r.updatedAt = note.getUpdatedAt();
        return r;
    }

    // Getter-Methoden
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isPublic() { return isPublic; }
    public boolean isEncrypted() { return isEncrypted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
