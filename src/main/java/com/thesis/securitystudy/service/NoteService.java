package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.util.EncryptionUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final String secretKey;

    @PersistenceContext
    private EntityManager entityManager;

    public NoteService(NoteRepository noteRepository,
                       @Value("${encryption.secret-key}") String secretKey) {
        this.noteRepository = noteRepository;
        this.secretKey = secretKey;
    }

    public NoteResponse createNote(String title, String content, boolean encrypt, User owner) {
        Note note = new Note();
        note.setTitle(title);
        note.setOwner(owner);

        applyContent(note, content, encrypt);

        Note saved = noteRepository.save(note);
        return toResponse(saved);
    }

    public List<NoteResponse> getNotes(User owner) {
        return noteRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long id, User owner) {
        Note note = findOwnedNote(id, owner);
        return toResponse(note);
    }

    public NoteResponse updateNote(Long id, String title, String content, boolean encrypt, User owner) {
        Note note = findOwnedNote(id, owner);

        note.setTitle(title);
        applyContent(note, content, encrypt);

        Note updated = noteRepository.save(note);
        return toResponse(updated);
    }

    public void deleteNote(Long id, User owner) {
        Note note = findOwnedNote(id, owner);
        noteRepository.delete(note);
    }

    private Note findOwnedNote(Long id, User owner) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        if (note.getOwner() == null
                || owner == null
                || note.getOwner().getId() == null
                || owner.getId() == null
                || !note.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
        }

        return note;
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
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    private NoteResponse toResponse(Note note) {
        Note readableNote = prepareReadableNote(note);
        return NoteResponse.from(readableNote);
    }

    private Note prepareReadableNote(Note note) {
        entityManager.detach(note);

        if (note.isEncrypted()) {
            try {
                note.setContent(EncryptionUtil.decrypt(note.getEncryptedContent(), secretKey));
            } catch (Exception e) {
                throw new IllegalStateException("Decryption failed", e);
            }
        }

        return note;
    }
}