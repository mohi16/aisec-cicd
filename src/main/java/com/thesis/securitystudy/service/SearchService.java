package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SearchService {

    private final NoteRepository noteRepository;

    public SearchService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<Note> search(String q, Boolean publicOnly, String sortBy, User currentUser) {
        List<Note> results;

        String query = q != null ? q.trim() : "";

        if (Boolean.TRUE.equals(publicOnly)) {
            if (query.isEmpty()) {
                results = noteRepository.findByIsPublicTrue();
            } else {
                results = noteRepository.searchPublicByQuery(query);
            }
        } else {
            if (query.isEmpty()) {
                results = noteRepository.findByOwnerOrderByCreatedAtDesc(currentUser);
            } else {
                results = noteRepository.searchByOwnerAndQuery(currentUser, query);
            }
        }

        sortResults(results, sortBy);
        return results;
    }

    private void sortResults(List<Note> notes, String sortBy) {
        String sortField = sortBy != null ? sortBy.trim() : "";

        if ("title".equalsIgnoreCase(sortField)) {
            notes.sort(Comparator.comparing(
                    note -> note.getTitle() == null ? "" : note.getTitle().toLowerCase()
            ));
            return;
        }

        if ("id".equalsIgnoreCase(sortField)) {
            notes.sort(Comparator.comparing(Note::getId));
            return;
        }

        notes.sort(Comparator.comparing(Note::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }
}