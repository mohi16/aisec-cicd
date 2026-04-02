package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public FileController(FileStorageService fileStorageService, UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file,
                                               Authentication authentication) {
        User user = getCurrentUser(authentication);
        FileEntity saved = fileStorageService.store(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(FileResponse.from(saved));
    }

    @GetMapping
    public List<FileResponse> list(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return fileStorageService.findAllByUser(user)
                .stream()
                .map(FileResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             Authentication authentication) {
        User user = getCurrentUser(authentication);

        FileEntity file = fileStorageService.findByIdForUser(id, user);
        Resource resource = fileStorageService.loadAsResource(file);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(file.getContentType());
            } catch (InvalidMediaTypeException ignored) {
                // fallback to octet-stream
            }
        }

        String contentDisposition = ContentDisposition.attachment()
                .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       Authentication authentication) {
        User user = getCurrentUser(authentication);
        fileStorageService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}