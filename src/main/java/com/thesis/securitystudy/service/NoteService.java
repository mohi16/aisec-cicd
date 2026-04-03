package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteRequest;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.exception.ResourceNotFoundException;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.util.EncryptionUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    public NoteService(NoteRepository noteRepository,
                       UserRepository userRepository,
                       EncryptionUtil encryptionUtil) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.encryptionUtil = encryptionUtil;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + auth.getName()));
    }

    private boolean isOwnerOrPublic(Note note, Authentication auth) {
        if (note.isPublic()) return true;
        if (note.getOwner() == null) return false;
        return note.getOwner().getUsername().equals(auth.getName());
    }

    private NoteResponse toResponse(Note note) {
        String content;
        if (note.isEncrypted()) {
            content = encryptionUtil.decrypt(Optional.ofNullable(note.getEncryptedContent()).orElse(""));
        } else {
            content = note.getContent();
        }
        NoteResponse r = new NoteResponse();
        // NoteResponse has private fields and no setters, but has a static from(Note).
        // Instead of mutating Note entity (which would be persisted if saved), we construct manually using reflection of fields:
        // To avoid reflection, we can set fields via creating a new NoteResponse instance through the static factory is not flexible.
        // But given existing NoteResponse has 'from' which reads note getters, we set content on a temporary object:
        // Simpler: create fields through constructor access — but NoteResponse has no public constructor with fields.
        // To keep it simple and avoid changing NoteResponse, populate a new NoteResponse by creating one and using reflection would be awkward.
        // Instead, we will copy logic from NoteResponse.from here (same fields) by using NoteResponse's package-private setters are not present.
        // So we create a new NoteResponse and use reflection to set private fields (allowed) OR (easier) we create the DTO manually
        // by constructing a new NoteResponse and returning it using the same field names via reflection.
        // For clarity and maintainability, we'll use a simple approach: create a NoteResponse and use a small helper to set fields via reflection.

        try {
            java.lang.reflect.Field idF = NoteResponse.class.getDeclaredField("id");
            java.lang.reflect.Field titleF = NoteResponse.class.getDeclaredField("title");
            java.lang.reflect.Field contentF = NoteResponse.class.getDeclaredField("content");
            java.lang.reflect.Field ownerF = NoteResponse.class.getDeclaredField("ownerUsername");
            java.lang.reflect.Field pubF = NoteResponse.class.getDeclaredField("isPublic");
            java.lang.reflect.Field encF = NoteResponse.class.getDeclaredField("isEncrypted");
            java.lang.reflect.Field createdF = NoteResponse.class.getDeclaredField("createdAt");
            java.lang.reflect.Field updatedF = NoteResponse.class.getDeclaredField("updatedAt");

            idF.setAccessible(true);
            titleF.setAccessible(true);
            contentF.setAccessible(true);
            ownerF.setAccessible(true);
            pubF.setAccessible(true);
            encF.setAccessible(true);
            createdF.setAccessible(true);
            updatedF.setAccessible(true);

            idF.set(r, note.getId());
            titleF.set(r, note.getTitle());
            contentF.set(r, content);
            ownerF.set(r, note.getOwner() != null ? note.getOwner().getUsername() : null);
            pubF.set(r, note.isPublic());
            encF.set(r, note.isEncrypted());
            createdF.set(r, note.getCreatedAt());
            updatedF.set(r, note.getUpdatedAt());
            return r;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to map Note to NoteResponse", ex);
        }
    }

    @Transactional
    public NoteResponse createNote(NoteRequest req, Authentication auth) {
        User user = getCurrentUser(auth);
        Note note = new Note();
        note.setTitle(req.getTitle());

        if (req.isEncrypt()) {
            String encrypted = encryptionUtil.encrypt(req.getContent());
            note.setEncrypted(true);
            note.setEncryptedContent(encrypted);
            note.setContent(null);
        } else {
            note.setEncrypted(false);
            note.setEncryptedContent(null);
            note.setContent(req.getContent());
        }
        note.setOwner(user);
        note.setPublic(req.isPublic());

        Note saved = noteRepository.save(note);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listNotes(Authentication auth) {
        User user = getCurrentUser(auth);
        List<Note> own = noteRepository.findByOwnerOrderByCreatedAtDesc(user);
        List<Note> pub = noteRepository.findByIsPublicTrue();

        Map<Long, Note> map = new LinkedHashMap<>();
        for (Note n : own) map.put(n.getId(), n);
        for (Note n : pub) map.putIfAbsent(n.getId(), n);

        return map.values().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(Long id, Authentication auth) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id " + id));
        if (!isOwnerOrPublic(note, auth)) {
            throw new AccessDeniedException("Not allowed to access this note");
        }
        return toResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(Long id, NoteRequest req, Authentication auth) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id " + id));
        if (!note.getOwner().getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Not allowed to update this note");
        }

        note.setTitle(req.getTitle());
        note.setPublic(req.isPublic());

        // Handle encryption transitions
        if (req.isEncrypt()) {
            // If already encrypted, either re-encrypt with new content or encrypt provided content
            String toEncrypt = req.getContent();
            String encrypted = encryptionUtil.encrypt(toEncrypt);
            note.setEncrypted(true);
            note.setEncryptedContent(encrypted);
            note.setContent(null);
        } else {
            // store plaintext
            note.setEncrypted(false);
            note.setEncryptedContent(null);
            note.setContent(req.getContent());
        }

        Note saved = noteRepository.save(note);
        return toResponse(saved);
    }

    @Transactional
    public void deleteNote(Long id, Authentication auth) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id " + id));
        if (!note.getOwner().getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Not allowed to delete this note");
        }
        noteRepository.delete(note);
    }
}
