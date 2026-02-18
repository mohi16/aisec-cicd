package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(name = "public", required = false, defaultValue = "false") boolean isPublic
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        List<Note> notes = searchService.search(q, isPublic, username);
        List<SearchResponse> result = notes.stream().map(SearchResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}