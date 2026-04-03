package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteRequest;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@Validated
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody NoteRequest req,
                                                                Authentication auth) {
        NoteResponse created = noteService.createNote(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note created", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> listNotes(Authentication auth) {
        List<NoteResponse> list = noteService.listNotes(auth);
        return ResponseEntity.ok(ApiResponse.ok("Notes", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(@PathVariable Long id, Authentication auth) {
        NoteResponse note = noteService.getNote(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Note", note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(@PathVariable Long id,
                                                                @Valid @RequestBody NoteRequest req,
                                                                Authentication auth) {
        NoteResponse updated = noteService.updateNote(id, req, auth);
        return ResponseEntity.ok(ApiResponse.ok("Note updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteNote(@PathVariable Long id,
                                                                       Authentication auth) {
        noteService.deleteNote(id, auth);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted", Map.of("id", id)));
    }
}
