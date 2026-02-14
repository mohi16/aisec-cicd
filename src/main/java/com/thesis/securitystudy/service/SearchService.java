package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public List<NoteResponse> searchOwnNotes(String query, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Search using native query for flexibility
        String sql = "SELECT * FROM notes WHERE owner_id = " + user.getId()
                + " AND (LOWER(title) LIKE '%" + query.toLowerCase() + "%'"
                + " OR LOWER(content) LIKE '%" + query.toLowerCase() + "%')"
                + " ORDER BY created_at DESC";

        @SuppressWarnings("unchecked")
        List<Note> notes = entityManager.createNativeQuery(sql, Note.class).getResultList();

        return notes.stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
    }

    public List<NoteResponse> searchPublicNotes(String query) {
        // Search public notes
        String sql = "SELECT * FROM notes WHERE is_public = true"
                + " AND (LOWER(title) LIKE '%" + query.toLowerCase() + "%'"
                + " OR LOWER(content) LIKE '%" + query.toLowerCase() + "%')"
                + " ORDER BY created_at DESC";

        @SuppressWarnings("unchecked")
        List<Note> notes = entityManager.createNativeQuery(sql, Note.class).getResultList();

        return notes.stream()
                .map(NoteResponse::from)
                .collect(Collectors.toList());
    }
}
