package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public SearchService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Suche Notizen. Wenn searchPublic == true werden nur öffentliche Notizen durchsucht,
     * sonst nur Notizen des gegebenen Benutzers.
     *
     * @param query Suchbegriff
     * @param searchPublic true -> öffentliche Notizen, false -> eigene Notizen
     * @param username aktueller Benutzername (zur Auflösung des Owner)
     * @return Liste gefundener Notizen
     */
    public List<Note> search(String query, boolean searchPublic, String username) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String q = query.trim();
        if (searchPublic) {
            return noteRepository.findByIsPublicTrueAndTitleContainingIgnoreCaseOrIsPublicTrueAndContentContainingIgnoreCase(q, q);
        } else {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            return noteRepository.findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(user, q, user, q);
        }
    }
}