package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "id"
    );

    @Autowired
    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Search notes according to parameters.
     * @param q search query (partial match on title or content). null -> match all
     * @param onlyPublic if true -> only search public notes; if null -> defaults to false (but unauthenticated requests treated as public-only)
     * @param sortBy field to sort by (must be whitelisted), default createdAt
     * @return list of matching notes
     */
    public List<Note> search(String q, Boolean onlyPublic, String sortBy) {
        String effectiveQ = q == null ? "" : q.trim();

        // choose sort field
        String sortField = "createdAt";
        if (sortBy != null && !sortBy.isBlank() && ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortField = sortBy;
        }
        Sort sort = Sort.by(Sort.Direction.DESC, sortField);

        // check authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        // If caller is not authenticated, force public-only
        if (!isAuthenticated) {
            return noteRepository.searchPublic(effectiveQ, sort);
        }

        // load User entity for authenticated principal
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            // fallback to public-only if user not found
            return noteRepository.searchPublic(effectiveQ, sort);
        }
        User user = userOpt.get();

        // if client explicitly asked for public-only, return public search
        if (Boolean.TRUE.equals(onlyPublic)) {
            return noteRepository.searchPublic(effectiveQ, sort);
        }

        // otherwise return notes visible to the owner (owner's notes OR public notes)
        return noteRepository.searchVisibleToOwner(user, effectiveQ, sort);
    }

}
