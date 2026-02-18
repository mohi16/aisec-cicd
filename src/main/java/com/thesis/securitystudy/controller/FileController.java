// src/main/java/com/thesis/securitystudy/controller/FileController.java
package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileMetadataDto;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;

    public FileController(FileService fileService, UserRepository userRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @PostMapping("/upload")
    public FileMetadataDto upload(@RequestParam("file") MultipartFile file, Principal principal) {
        User user = resolveCurrentUser(principal);
        FileEntity saved = fileService.store(file, user);

        return new FileMetadataDto(
                saved.getId(),
                saved.getOriginalFilename(),
                saved.getContentType(),
                saved.getFileSize(),
                toOffsetDateTime(saved.getCreatedAt())
        );
    }

    @GetMapping
    public List<FileMetadataDto> list(Principal principal) {
        User user = resolveCurrentUser(principal);
        List<FileEntity> files = fileService.listByUploader(user);

        return files.stream()
                .map(f -> new FileMetadataDto(
                        f.getId(),
                        f.getOriginalFilename(),
                        f.getContentType(),
                        f.getFileSize(),
                        toOffsetDateTime(f.getCreatedAt())
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Principal principal) {
        FileEntity entity = fileService.getById(id);
        if (!entity.getUploader().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not owner");
        }

        Resource resource = fileService.loadAsResource(entity);
        String filename = entity.getOriginalFilename();
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(entity.getContentType() != null
                        ? MediaType.parseMediaType(entity.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        FileEntity entity = fileService.getById(id);
        if (!entity.getUploader().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not owner");
        }

        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
