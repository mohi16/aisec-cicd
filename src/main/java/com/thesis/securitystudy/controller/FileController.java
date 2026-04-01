package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public FileController(FileStorageService fileStorageService,
                          UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                   Authentication authentication) {
        User owner = resolveCurrentUser(authentication);
        FileEntity savedEntity = fileStorageService.storeFile(file, owner);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/{id}/download")
                .buildAndExpand(savedEntity.getId())
                .toUri();

        return ResponseEntity.created(location).body(FileResponse.from(savedEntity));
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(Authentication authentication) {
        User owner = resolveCurrentUser(authentication);

        List<FileResponse> files = fileStorageService.listFiles(owner)
                .stream()
                .map(FileResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") Long id,
                                                 Authentication authentication) {
        User owner = resolveCurrentUser(authentication);

        FileEntity fileEntity = fileStorageService.getFile(id, owner);
        Resource resource = fileStorageService.loadFile(id, owner);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileEntity.getContentType() != null && !fileEntity.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(fileEntity.getContentType());
            } catch (InvalidMediaTypeException ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable("id") Long id,
                                           Authentication authentication) {
        User owner = resolveCurrentUser(authentication);
        fileStorageService.deleteFile(id, owner);
        return ResponseEntity.noContent().build();
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
    }
}