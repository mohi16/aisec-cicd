package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.exception.ResourceNotFoundException;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    @Value("${encryption.secret-key}")
    private String secretKey;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public NoteResponse createNote(String title, String content, boolean isPublic, boolean encrypt, User owner) throws Exception {
        Note note = new Note();
        note.setTitle(title);
        note.setOwner(owner);
        note.setPublic(isPublic);
        applyContent(note, content, encrypt);

        Note saved = noteRepository.save(note);
        return toResponse(saved);
    }

    public List<NoteResponse> listNotes(User owner) {
        return noteRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    public NoteResponse getNoteById(Long id, User requester) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        boolean canRead = note.getOwner().getId().equals(requester.getId()) || note.isPublic();
        if (!canRead) {
            throw new ResourceNotFoundException("Note not found");
        }

        return toResponse(note);
    }

    public NoteResponse updateNote(Long id, String title, String content, boolean isPublic, boolean encrypt, User requester) throws Exception {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        ensureOwner(note, requester);

        note.setTitle(title);
        note.setPublic(isPublic);
        applyContent(note, content, encrypt);

        Note updated = noteRepository.save(note);
        return toResponse(updated);
    }

    public void deleteNote(Long id, User requester) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        ensureOwner(note, requester);
        noteRepository.delete(note);
    }

    private void ensureOwner(Note note, User requester) {
        if (!note.getOwner().getId().equals(requester.getId())) {
            throw new ResourceNotFoundException("Note not found");
        }
    }

    private void applyContent(Note note, String content, boolean encrypt) {
        try {
            if (encrypt) {
                String encrypted = EncryptionUtil.encrypt(content, secretKey);
                note.setEncryptedContent(encrypted);
                note.setContent(null);
                note.setEncrypted(true);
            } else {
                note.setContent(content);
                note.setEncryptedContent(null);
                note.setEncrypted(false);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt note content", e);
        }
    }

    private NoteResponse toResponse(Note note) {
        try {
            if (note.isEncrypted()) {
                note.setContent(EncryptionUtil.decrypt(note.getEncryptedContent(), secretKey));
            }
            return NoteResponse.from(note);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt note content", e);
        }
    }
}
