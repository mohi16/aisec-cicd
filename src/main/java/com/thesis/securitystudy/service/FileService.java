// src/main/java/com/thesis/securitystudy/service/FileService.java
package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final Path uploadRoot;

    public FileService(FileRepository fileRepository, @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.fileRepository = fileRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public FileEntity store(MultipartFile file, User uploader) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        String original = Paths.get(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename())
                .getFileName()
                .toString();
        String stored = UUID.randomUUID().toString() + "_" + original;
        Path target = uploadRoot.resolve(stored).normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }

        FileEntity entity = new FileEntity();
        entity.setOriginalFilename(original);
        entity.setStoredFilename(stored);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setStoragePath(target.toString());
        entity.setUploader(uploader);

        return fileRepository.save(entity);
    }

    public List<FileEntity> listByUploader(User uploader) {
        return fileRepository.findByUploaderOrderByCreatedAtDesc(uploader);
    }

    public FileEntity getById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    public Resource loadAsResource(FileEntity entity) {
        try {
            Path path = Paths.get(entity.getStoragePath());
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not readable");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file", e);
        }
    }

    public void delete(Long id) {
        FileEntity entity = getById(id);
        try {
            Path path = Paths.get(entity.getStoragePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file", e);
        }
        fileRepository.delete(entity);
    }
}
