package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean encrypt,
            @AuthenticationPrincipal User owner) throws Exception {
        return ResponseEntity.ok(noteService.createNote(title, content, encrypt, owner));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(@AuthenticationPrincipal User owner) throws Exception {
        return ResponseEntity.ok(noteService.getNotes(owner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(
            @PathVariable Long id,
            @AuthenticationPrincipal User owner) throws Exception {
        return ResponseEntity.ok(noteService.getNoteById(id, owner));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @RequestParam String content,
            @RequestParam(defaultValue = "false") boolean encrypt,
            @AuthenticationPrincipal User owner) throws Exception {
        return ResponseEntity.ok(noteService.updateNote(id, content, encrypt, owner));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable Long id, @AuthenticationPrincipal User owner) {
        noteService.deleteNoteById(id, owner);
        return ResponseEntity.noContent().build();
    }
}
