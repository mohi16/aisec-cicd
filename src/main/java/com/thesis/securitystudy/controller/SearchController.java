package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;
    private final UserRepository userRepository;

    public SearchController(SearchService searchService, UserRepository userRepository) {
        this.searchService = searchService;
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResponse>>> searchNotes(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "public", required = false) Boolean publicOnly,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            Authentication authentication
    ) {
        User currentUser = resolveCurrentUser(authentication);

        List<Note> results = searchService.search(q, publicOnly, sortBy, currentUser);

        List<SearchResponse> payload = results.stream()
                .map(SearchResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Search results", payload));
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}