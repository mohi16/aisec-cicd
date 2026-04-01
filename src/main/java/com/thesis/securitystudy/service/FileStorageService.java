package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class FileStorageService {

    private final FileRepository fileRepository;
    private final Path uploadRoot;

    public FileStorageService(FileRepository fileRepository,
                              @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.fileRepository = fileRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not initialize upload directory",
                    e
            );
        }
    }

    public FileEntity storeFile(MultipartFile file, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );

        if (originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename.");
        }

        FileEntity entity = new FileEntity();
        entity.setOriginalFilename(originalFilename);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploader(uploader);

        FileEntity savedEntity = fileRepository.save(entity);
        Path destinationFile = resolveStoredPath(savedEntity);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return savedEntity;
        } catch (IOException e) {
            fileRepository.deleteById(savedEntity.getId());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file.",
                    e
            );
        }
    }

    public List<FileEntity> listFiles(User uploader) {
        return fileRepository.findByUploaderOrderByCreatedAtDesc(uploader);
    }

    public FileEntity getFile(Long id, User uploader) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (fileEntity.getUploader() == null
                || fileEntity.getUploader().getId() == null
                || uploader == null
                || uploader.getId() == null
                || !fileEntity.getUploader().getId().equals(uploader.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return fileEntity;
    }

    public Resource loadFile(Long id, User uploader) {
        FileEntity fileEntity = getFile(id, uploader);
        Path filePath = resolveStoredPath(fileEntity);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not read file.");
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not read file.", e);
        }
    }

    public void deleteFile(Long id, User uploader) {
        FileEntity fileEntity = getFile(id, uploader);
        Path filePath = resolveStoredPath(fileEntity);

        try {
            Files.deleteIfExists(filePath);
            fileRepository.delete(fileEntity);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete file.",
                    e
            );
        }
    }

    private Path resolveStoredPath(FileEntity fileEntity) {
        Path destinationFile = uploadRoot
                .resolve(Paths.get(fileEntity.getId() + "_" + fileEntity.getOriginalFilename()))
                .normalize()
                .toAbsolutePath();

        if (!destinationFile.getParent().equals(uploadRoot)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot store file outside current directory."
            );
        }

        return destinationFile;
    }
}