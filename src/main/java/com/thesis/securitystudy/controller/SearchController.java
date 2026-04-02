package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/notes/search?q=<query>&public=true&sortBy=<field>
     *
     * - q (required): search query
     * - public (optional): if true return only public notes; otherwise return public + owned (if authenticated)
     * - sortBy (optional): title | createdAt | updatedAt | id
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "public", required = false) Boolean publicOnly,
            @RequestParam(value = "sortBy", required = false) String sortBy
    ) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<SearchResponse> results = searchService.search(q, publicOnly, sortBy);
        return ResponseEntity.ok(results);
    }
}
