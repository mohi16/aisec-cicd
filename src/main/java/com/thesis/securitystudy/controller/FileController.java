package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storageService;
    private final UserRepository userRepository;

    public FileController(FileStorageService storageService, UserRepository userRepository) {
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponse>> uploadFile(@RequestParam("file") MultipartFile file,
                                                                Authentication auth) throws IOException {
        User user = resolveUser(auth);
        FileEntity fe = storageService.store(file, user);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded", FileResponse.from(fe)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileResponse>>> listFiles(Authentication auth) {
        User user = resolveUser(auth);
        List<FileResponse> files = storageService.listByUser(user).stream()
                .map(FileResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Files", files));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication auth) throws IOException {
        User user = resolveUser(auth);
        Resource resource = storageService.loadAsResource(id, user);

        // Find FileEntity to get original filename and content type
        FileEntity fe = storageService.listByUser(user).stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IOException("File not found"));

        String contentType = fe.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : fe.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fe.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable Long id, Authentication auth) throws IOException {
        User user = resolveUser(auth);
        storageService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.ok("File deleted"));
    }
}

