package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteRequest;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.Note;
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
import java.util.stream.Collectors;

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
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody NoteRequest request,
                                                                Authentication auth) {
        User owner = resolveCurrentUser(auth);
        Note created = noteService.createNote(request.getTitle(), request.getContent(), request.isEncrypt(), owner);
        NoteResponse resp = NoteResponse.from(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note created", resp));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> listNotes(Authentication auth) {
        User owner = resolveCurrentUser(auth);
        List<Note> notes = noteService.getNotes(owner);
        List<NoteResponse> responses = notes.stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Notes fetched", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNote(@PathVariable("id") Long id,
                                                             Authentication auth) {
        User owner = resolveCurrentUser(auth);
        Note note = noteService.getNoteById(id, owner);
        return ResponseEntity.ok(ApiResponse.ok("Note fetched", NoteResponse.from(note)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(@PathVariable("id") Long id,
                                                                @Valid @RequestBody NoteRequest request,
                                                                Authentication auth) {
        User owner = resolveCurrentUser(auth);
        Note updated = noteService.updateNote(id, request.getContent(), request.isEncrypt(), owner);
        return ResponseEntity.ok(ApiResponse.ok("Note updated", NoteResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable("id") Long id,
                                                        Authentication auth) {
        User owner = resolveCurrentUser(auth);
        noteService.deleteNote(id, owner);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted"));
    }
}
