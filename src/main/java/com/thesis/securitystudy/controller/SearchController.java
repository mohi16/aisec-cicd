package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/notes/search")
    public ResponseEntity<List<Note>> searchNotes(@RequestParam String q, @RequestParam(required = false) Boolean publicOnly, Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        List<Note> results;

        if (Boolean.TRUE.equals(publicOnly)) {
            results = searchService.searchPublicNotes(q);
        } else {
            results = searchService.searchOwnNotes(currentUser, q);
        }

        return ResponseEntity.ok(results);
    }
}
