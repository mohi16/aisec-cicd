package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    // Inject repositories / utilities as needed when you implement the TODOs.
    public NoteService() {
    }

    /**
     * Create a new Note. If encrypt == true, caller is expected to use EncryptionUtil
     * and set encryptedContent / isEncrypted accordingly.
     */
    public Note createNote(String title, String content, boolean encrypt, User owner) {
        // TODO: implement persistence and optional encryption
        throw new UnsupportedOperationException("TODO implement createNote");
    }

    /**
     * Return all notes belonging to owner (e.g. ordered by createdAt desc).
     */
    public List<Note> getNotes(User owner) {
        // TODO: implement fetching notes for owner
        throw new UnsupportedOperationException("TODO implement getNotes");
    }

    /**
     * Return a single note by id. Must enforce owner-only access (or throw an exception).
     */
    public Note getNoteById(Long id, User owner) {
        // TODO: implement retrieval and ownership check
        throw new UnsupportedOperationException("TODO implement getNoteById");
    }

    /**
     * Update note content (and optionally re-encrypt). Must enforce owner-only access.
     */
    public Note updateNote(Long id, String content, boolean encrypt, User owner) {
        // TODO: implement update logic, encryption handling
        throw new UnsupportedOperationException("TODO implement updateNote");
    }

    /**
     * Delete note by id. Must enforce owner-only access.
     */
    public void deleteNote(Long id, User owner) {
        // TODO: implement deletion with ownership check
        throw new UnsupportedOperationException("TODO implement deleteNote");
    }
}
