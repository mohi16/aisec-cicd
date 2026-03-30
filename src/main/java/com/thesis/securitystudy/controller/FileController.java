package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.FileResponse;
import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import com.thesis.securitystudy.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                   @AuthenticationPrincipal User uploader) throws IOException {
        String storedFilename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String storagePath = fileStorageService.saveFile(file, storedFilename);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFilename(file.getOriginalFilename());
        fileEntity.setStoredFilename(storedFilename);
        fileEntity.setContentType(file.getContentType());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setStoragePath(storagePath);
        fileEntity.setUploader(uploader);

        fileRepository.save(fileEntity);

        FileResponse response = new FileResponse(fileEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<FileResponse> listFiles(@AuthenticationPrincipal User uploader) {
        return fileRepository.findByUploaderOrderByCreatedAtDesc(uploader)
                .stream()
                .map(FileResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id,
                                               @AuthenticationPrincipal User uploader) throws IOException {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!fileEntity.getUploader().equals(uploader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        File file = fileStorageService.loadFile(fileEntity.getStoredFilename());
        byte[] fileContent = new FileInputStream(file).readAllBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, fileEntity.getContentType())
                .body(fileContent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id,
                                           @AuthenticationPrincipal User uploader) throws IOException {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!fileEntity.getUploader().equals(uploader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        fileStorageService.deleteFile(fileEntity.getStoredFilename());
        fileRepository.delete(fileEntity);

        return ResponseEntity.noContent().build();
    }
}
