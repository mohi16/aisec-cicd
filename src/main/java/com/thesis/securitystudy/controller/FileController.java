package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storageService;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    public FileController(FileStorageService storageService,
                          UserRepository userRepository,
                          FileRepository fileRepository) {
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        User user = getCurrentUser(authentication);
        FileEntity stored = storageService.storeFile(file, user);
        return ResponseEntity.status(CREATED).body(toDto(stored));
    }

    @GetMapping
    public List<FileResponse> list(Authentication authentication) {
        User user = getCurrentUser(authentication);
        // Return files owned by current user, ordered by createdAt desc if repository method exists
        List<FileEntity> files = fileRepository.findByUploaderOrderByCreatedAtDesc(user);
        return files.stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        FileEntity fe = storageService.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "File not found"));
        if (!fe.getUploader().getId().equals(user.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to download this file");
        }

        Resource resource = storageService.loadAsResourceByStoredFilename(fe.getStoredFilename());
        String contentType = fe.getContentType() != null ? fe.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // Ensure proper filename in header; basic escaping
        String originalFilename = fe.getOriginalFilename() == null ? "file" : fe.getOriginalFilename();
        String encoded = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename=\"" + originalFilename.replace("\"", "'") + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        FileEntity fe = storageService.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "File not found"));
        if (!fe.getUploader().getId().equals(user.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Not allowed to delete this file");
        }
        storageService.deleteFile(fe);
        return ResponseEntity.noContent().build();
    }

    private FileResponse toDto(FileEntity fe) {
        FileResponse dto = new FileResponse();
        dto.setId(fe.getId());
        dto.setOriginalFilename(fe.getOriginalFilename());
        dto.setContentType(fe.getContentType());
        dto.setFileSize(fe.getFileSize());
        // If BaseEntity provides createdAt
        try {
            java.lang.reflect.Method m = fe.getClass().getMethod("getCreatedAt");
            Object created = m.invoke(fe);
            if (created instanceof java.time.Instant) dto.setCreatedAt((Instant) created);
        } catch (Exception ignored) {}
        if (fe.getUploader() != null) dto.setUploaderUsername(fe.getUploader().getUsername());
        return dto;
    }
}
