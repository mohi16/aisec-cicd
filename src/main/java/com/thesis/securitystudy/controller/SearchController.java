package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> searchNotes(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "public", required = false, defaultValue = "false") boolean publicOnly,
            Authentication authentication) {

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Query parameter 'q' is required"));
        }

        String username = (authentication != null) ? authentication.getName() : null;
        List<NoteResponse> results = searchService.searchNotes(q, publicOnly, username);
        return ResponseEntity.ok(ApiResponse.ok("Search results", results));
    }
}

