package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Search notes for the given username.
     * If publicOnly is true, search only public notes; otherwise search only notes owned by the user.
     * Returns a list of NoteResponse DTOs.
     */
    public List<NoteResponse> searchNotes(String q, boolean publicOnly, String username) {
        String safeQ = (q == null) ? "" : q.trim();
        if (safeQ.length() > 200) {
            safeQ = safeQ.substring(0, 200);
        }

        if (publicOnly) {
            List<Note> notes = noteRepository.searchPublicByQuery(safeQ);
            return notes.stream().map(NoteResponse::from).collect(Collectors.toList());
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();
        List<Note> notes = noteRepository.searchByOwnerAndQuery(user, safeQ);
        return notes.stream().map(NoteResponse::from).collect(Collectors.toList());
    }
}

