package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.SearchResponse;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/notes/search?q=<query>&public=true&sortBy=<field>
     * - q: partial match against title and content (case-insensitive)
     * - public: if true -> search only public notes; if omitted -> authenticated users see their own notes + public notes; unauthenticated users see only public
     * - sortBy: optional, allowed fields: createdAt, updatedAt, title, id. Default: createdAt desc
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchResponse>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "public", required = false) Boolean onlyPublic,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        List<Note> notes = searchService.search(q, onlyPublic, sortBy);
        List<SearchResponse> resp = notes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    private SearchResponse toDto(Note n) {
        SearchResponse dto = new SearchResponse();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setPublic(n.isPublic());
        dto.setEncrypted(n.isEncrypted());
        dto.setOwnerUsername(n.getOwner() != null ? n.getOwner().getUsername() : null);
        // if encrypted return encryptedContent, hide plaintext
        if (n.isEncrypted()) {
            dto.setEncryptedContent(n.getEncryptedContent());
            dto.setContent(null);
        } else {
            dto.setContent(n.getContent());
            dto.setEncryptedContent(null);
        }
        // createdAt/updatedAt come from BaseEntity (assumed)
        try {
            dto.setCreatedAt(n.getCreatedAt());
            dto.setUpdatedAt(n.getUpdatedAt());
        } catch (Exception e) {
            // if BaseEntity doesn't expose these fields as expected, ignore silently
        }
        return dto;
    }
}
