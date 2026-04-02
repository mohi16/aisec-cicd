package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteRequest;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final UserRepository userRepository;

    public NoteController(NoteService noteService, UserRepository userRepository) {
        this.noteService = noteService;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody NoteRequest request,
                                                                Authentication auth) {
        User owner = resolveCurrentUser(auth);
        NoteResponse created = noteService.createNote(
                request.getTitle(),
                request.getContent(),
                request.isEncrypt(),
                owner
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note created", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> listNotes(Authentication auth) {
        User owner = resolveCurrentUser(auth);
        List<NoteResponse> responses = noteService.getNotes(owner);
        return ResponseEntity.ok(ApiResponse.ok("Notes fetched", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(@PathVariable("id") Long id,
                                                             Authentication auth) {
        User owner = resolveCurrentUser(auth);
        NoteResponse note = noteService.getNoteById(id, owner);
        return ResponseEntity.ok(ApiResponse.ok("Note fetched", note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(@PathVariable("id") Long id,
                                                                @Valid @RequestBody NoteRequest request,
                                                                Authentication auth) {
        User owner = resolveCurrentUser(auth);
        NoteResponse updated = noteService.updateNote(
                id,
                request.getTitle(),
                request.getContent(),
                request.isEncrypt(),
                owner
        );

        return ResponseEntity.ok(ApiResponse.ok("Note updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable("id") Long id,
                                                        Authentication auth) {
        User owner = resolveCurrentUser(auth);
        noteService.deleteNote(id, owner);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted"));
    }
}