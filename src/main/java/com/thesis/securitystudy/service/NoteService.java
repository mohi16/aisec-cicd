package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    @Value("${encryption.secret-key}")
    private String secretKey;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public NoteResponse createNote(String title, String content, boolean encrypt, User owner) throws Exception {
        Note note = new Note();
        note.setTitle(title);
        note.setOwner(owner);

        if (encrypt) {
            note.setEncryptedContent(EncryptionUtil.encrypt(content, secretKey));
            note.setEncrypted(true);
        } else {
            note.setContent(content);
        }

        note = noteRepository.save(note);
        return NoteResponse.from(note);
    }

    public List<NoteResponse> getNotes(User owner) throws Exception {
        return noteRepository.findByOwner(owner).stream()
                .map(note -> {
                    if (note.isEncrypted()) {
                        try {
                            note.setContent(EncryptionUtil.decrypt(note.getEncryptedContent(), secretKey));
                        } catch (Exception e) {
                            throw new RuntimeException("Decryption failed", e);
                        }
                    }
                    return NoteResponse.from(note);
                })
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long id, User owner) throws Exception {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (!note.getOwner().equals(owner)) {
            throw new RuntimeException("Access denied");
        }

        if (note.isEncrypted()) {
            note.setContent(EncryptionUtil.decrypt(note.getEncryptedContent(), secretKey));
        }

        return NoteResponse.from(note);
    }

    public NoteResponse updateNote(Long id, String content, boolean encrypt, User owner) throws Exception {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (!note.getOwner().equals(owner)) {
            throw new RuntimeException("Access denied");
        }

        if (encrypt) {
            note.setEncryptedContent(EncryptionUtil.encrypt(content, secretKey));
            note.setEncrypted(true);
            note.setContent(null);
        } else {
            note.setContent(content);
            note.setEncrypted(false);
            note.setEncryptedContent(null);
        }

        note = noteRepository.save(note);
        return NoteResponse.from(note);
    }

    public void deleteNoteById(Long id, User owner) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (!note.getOwner().equals(owner)) {
            throw new RuntimeException("Access denied");
        }

        noteRepository.delete(note);
    }
}
