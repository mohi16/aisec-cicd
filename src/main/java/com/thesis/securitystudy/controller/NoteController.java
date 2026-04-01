package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteRequest;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.exception.ResourceNotFoundException;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(
            @Valid @RequestBody NoteRequest request,
            Authentication auth
    ) throws Exception {
        User owner = getCurrentUser(auth);
        NoteResponse response = noteService.createNote(
                request.getTitle(),
                request.getContent(),
                request.isPublic(),
                request.isEncrypt(),
                owner
        );
        return ResponseEntity.ok(ApiResponse.ok("Note created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> listNotes(Authentication auth) {
        User owner = getCurrentUser(auth);
        List<NoteResponse> notes = noteService.listNotes(owner);
        return ResponseEntity.ok(ApiResponse.ok("Notes retrieved successfully", notes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNoteById(@PathVariable Long id, Authentication auth) {
        User requester = getCurrentUser(auth);
        NoteResponse note = noteService.getNoteById(id, requester);
        return ResponseEntity.ok(ApiResponse.ok("Note retrieved successfully", note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request,
            Authentication auth
    ) throws Exception {
        User requester = getCurrentUser(auth);
        NoteResponse updated = noteService.updateNote(
                id,
                request.getTitle(),
                request.getContent(),
                request.isPublic(),
                request.isEncrypt(),
                requester
        );
        return ResponseEntity.ok(ApiResponse.ok("Note updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long id, Authentication auth) {
        User requester = getCurrentUser(auth);
        noteService.deleteNote(id, requester);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted successfully"));
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
