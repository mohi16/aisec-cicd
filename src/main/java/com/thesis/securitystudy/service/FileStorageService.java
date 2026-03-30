package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private final FileRepository fileRepository;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir,
                              FileRepository fileRepository) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }
    }

    @Transactional
    public FileEntity storeFile(MultipartFile file, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        try {
            String originalFilename = Path.of(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).getFileName().toString();
            String ext = "";
            int idx = originalFilename.lastIndexOf('.');
            if (idx >= 0) ext = originalFilename.substring(idx);

            String storedFilename = UUID.randomUUID().toString() + ext;
            Path target = uploadDir.resolve(storedFilename);

            // Save file to disk
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Persist metadata
            FileEntity fe = new FileEntity();
            fe.setOriginalFilename(originalFilename);
            fe.setStoredFilename(storedFilename);
            fe.setContentType(file.getContentType());
            fe.setFileSize(file.getSize());
            fe.setStoragePath(target.toString());
            fe.setUploader(uploader);
            // createdAt etc. should be handled by BaseEntity (if present)
            return fileRepository.save(fe);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file", ex);
        }
    }

    public Resource loadAsResourceByStoredFilename(String storedFilename) {
        try {
            Path file = uploadDir.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File URL error", ex);
        }
    }

    public Optional<FileEntity> findById(Long id) {
        return fileRepository.findById(id);
    }

    @Transactional
    public void deleteFile(FileEntity fileEntity) {
        // delete file from disk
        try {
            Path p = Paths.get(fileEntity.getStoragePath());
            Files.deleteIfExists(p);
        } catch (IOException ex) {
            // Log and continue with DB delete (or decide to fail)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete stored file", ex);
        }

        // delete metadata
        fileRepository.delete(fileEntity);
    }
}
