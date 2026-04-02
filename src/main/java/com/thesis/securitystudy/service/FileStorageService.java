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

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

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
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public FileEntity store(MultipartFile multipartFile, User user) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        if (multipartFile.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is too large");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename is missing");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename);

        if (cleanedFilename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        FileEntity file = new FileEntity();
        file.setOriginalFilename(cleanedFilename);
        file.setContentType(multipartFile.getContentType());
        file.setFileSize(multipartFile.getSize());
        file.setUploader(user);

        FileEntity saved = fileRepository.save(file);
        Path targetPath = resolvePath(saved);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return saved;
        } catch (IOException e) {
            fileRepository.deleteById(saved.getId());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
    }

    public List<FileEntity> findAllByUser(User user) {
        return fileRepository.findByUploaderOrderByCreatedAtDesc(user);
    }

    public FileEntity findByIdForUser(Long id, User user) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (file.getUploader() == null || user == null || file.getUploader().getId() == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        if (!file.getUploader().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return file;
    }

    public Resource loadAsResource(FileEntity file) {
        Path filePath = resolvePath(file);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not load file");
        }
    }

    public void delete(Long id, User user) {
        FileEntity file = findByIdForUser(id, user);
        Path filePath = resolvePath(file);

        try {
            Files.deleteIfExists(filePath);
            fileRepository.delete(file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete file");
        }
    }

    private Path resolvePath(FileEntity file) {
        Path path = uploadRoot.resolve(file.getId() + "_" + file.getOriginalFilename())
                .normalize()
                .toAbsolutePath();

        if (!path.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        return path;
    }
}