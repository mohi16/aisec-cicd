package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final NoteRepository noteRepository;

    public SearchService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<Note> searchOwnNotes(User user, String query) {
        return noteRepository.findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(user, query, user, query);
    }

    public List<Note> searchPublicNotes(String query) {
        return noteRepository.searchPublicNotes(query);
    }
}
