package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    // Du kannst hier bei Bedarf Repositories via Konstruktor injizieren (NoteRepository, UserRepository, ...)
    public SearchService() {
        // TODO: inject repositories via constructor if needed
    }

    /**
     * Search notes based on query and visibility.
     *
     * @param q           free-text query (may be null or empty)
     * @param publicOnly  if true -> only public notes; if false -> only user's notes; if null -> both with filtering rules
     * @param sortBy      sorting key (e.g. "createdAt", "title"), may be null
     * @param currentUser current authenticated User (may be null for unauthenticated searches)
     * @return list of matching Note entities (TODO: implement)
     */
    public List<Note> search(String q, Boolean publicOnly, String sortBy, User currentUser) {
        // TODO: implement search logic and visibility filtering
        throw new UnsupportedOperationException("Search not implemented yet");
    }
}
