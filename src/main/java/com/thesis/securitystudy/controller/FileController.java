package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                   Authentication authentication) {
        // TODO: resolve authenticated principal to com.thesis.securitystudy.model.User
        // TODO: call fileStorageService.storeFile(file, owner)
        // TODO: return ResponseEntity.created(...).body(FileResponse.from(savedEntity))
        throw new UnsupportedOperationException("Not implemented");
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(Authentication authentication) {
        // TODO: resolve authenticated principal to com.thesis.securitystudy.model.User
        // TODO: call fileStorageService.listFiles(owner) and map to FileResponse
        throw new UnsupportedOperationException("Not implemented");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") Long id,
                                                 Authentication authentication) {
        // TODO: resolve authenticated principal to com.thesis.securitystudy.model.User
        // TODO: Resource resource = fileStorageService.loadFile(id, owner);
        // TODO: set Content-Disposition and content type headers
        throw new UnsupportedOperationException("Not implemented");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable("id") Long id,
                                           Authentication authentication) {
        // TODO: resolve authenticated principal to com.thesis.securitystudy.model.User
        // TODO: fileStorageService.deleteFile(id, owner);
        // TODO: return ResponseEntity.noContent().build();
        throw new UnsupportedOperationException("Not implemented");
    }
}
