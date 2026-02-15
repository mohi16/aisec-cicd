package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public List<SearchResponse> search(String q, boolean publicOnly, Authentication auth) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'q' must not be empty");
        }

        if (publicOnly) {
            List<Note> notes = noteRepository.searchPublicByQuery(trimmed);
            return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        } else {
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            List<Note> notes = noteRepository.searchByOwnerAndQuery(user, trimmed);
            return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        }
    }
}

