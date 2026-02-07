package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Note;
import java.time.LocalDateTime;

public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private String ownerUsername;
    private boolean isPublic;
    private boolean isEncrypted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoteResponse from(Note note) {
        NoteResponse r = new NoteResponse();
        r.id = note.getId();
        r.title = note.getTitle();
        r.content = note.getContent();
        r.ownerUsername = note.getOwner().getUsername();
        r.isPublic = note.isPublic();
        r.isEncrypted = note.isEncrypted();
        r.createdAt = note.getCreatedAt();
        r.updatedAt = note.getUpdatedAt();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isPublic() { return isPublic; }
    public boolean isEncrypted() { return isEncrypted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
