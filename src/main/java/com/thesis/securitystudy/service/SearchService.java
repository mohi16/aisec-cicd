package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    // allowed sort fields to avoid arbitrary property injection
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "createdAt", "updatedAt", "id"
    );

    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public List<SearchResponse> search(String q, Boolean publicOnly, String sortBy) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            return Collections.emptyList();
        }

        Sort sort = buildSort(sortBy);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        // if client explicitly requests public=true -> only public results
        if (Boolean.TRUE.equals(publicOnly)) {
            List<Note> notes = noteRepository.searchPublic(query, sort);
            return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        }

        // if not authenticated -> only public notes
        if (!isAuthenticated) {
            List<Note> notes = noteRepository.searchPublic(query, sort);
            return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        }

        // authenticated: return notes that are public OR owned by the user
        String username = auth.getName();
        Optional<User> maybeUser = userRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            // fallback to public if user not found
            List<Note> notes = noteRepository.searchPublic(query, sort);
            return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        }

        Long ownerId = maybeUser.get().getId();
        List<Note> notes = noteRepository.searchVisibleToUser(ownerId, query, sort);
        return notes.stream().map(SearchResponse::from).collect(Collectors.toList());
    }

    private Sort buildSort(String sortBy) {
        String field = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        // default descending for createdAt, ascending otherwise
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "createdAt";
        }

        if ("createdAt".equals(field) || "updatedAt".equals(field)) {
            return Sort.by(Sort.Direction.DESC, field);
        } else {
            return Sort.by(Sort.Direction.ASC, field);
        }
    }
}
