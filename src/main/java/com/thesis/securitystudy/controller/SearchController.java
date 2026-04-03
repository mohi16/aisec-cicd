package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/notes/search
     * Query params:
     *  - q (String)         : search query (optional)
     *  - public (Boolean)   : if true, search only public notes; if false, only private/own notes; if omitted, combined (optional)
     *  - sortBy (String)    : e.g. "createdAt" or "title" (optional)
     *
     * NOTE: method body left as TODO — implement mapping from Authentication to User,
     * call searchService.search(...), convert Note -> SearchResponse, return ApiResponse.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResponse>>> searchNotes(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "public", required = false) Boolean publicOnly,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            Authentication authentication
    ) {
        // TODO: Resolve current authenticated User instance from `authentication`
        // User currentUser = ...;

        // TODO: Call searchService.search(q, publicOnly, sortBy, currentUser)
        // List<Note> results = searchService.search(q, publicOnly, sortBy, currentUser);

        // TODO: Map to SearchResponse and return
        // List<SearchResponse> payload = results.stream().map(SearchResponse::from).collect(Collectors.toList());
        // return ResponseEntity.ok(ApiResponse.ok("Search results", payload));

        throw new UnsupportedOperationException("Not implemented yet");
    }
}
