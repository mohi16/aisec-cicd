package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.NoteResponse;
import com.thesis.securitystudy.service.SearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> searchNotes(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "false") boolean isPublic,
            Authentication authentication) {

        List<NoteResponse> results;

        if (isPublic) {
            results = searchService.searchPublicNotes(q);
        } else {
            results = searchService.searchOwnNotes(q, authentication.getName());
        }

        return ResponseEntity.ok(ApiResponse.ok("Search results", results));
    }
}
