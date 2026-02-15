package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class FileStorageService {

    private final Path uploadDir;
    private final FileRepository fileRepository;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir,
                              FileRepository fileRepository) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;

        Files.createDirectories(this.uploadDir);
    }

    public FileEntity store(MultipartFile file, User uploader) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String stored = System.currentTimeMillis() + "-" + original;

        if (original.contains("..")) {
            throw new IOException("Invalid path sequence in filename: " + original);
        }

        Path target = this.uploadDir.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        FileEntity fe = new FileEntity();
        fe.setOriginalFilename(original);
        fe.setStoredFilename(stored);
        fe.setContentType(file.getContentType());
        fe.setFileSize(file.getSize());
        fe.setStoragePath(target.toString());
        fe.setUploader(uploader);

        return fileRepository.save(fe);
    }

    public List<FileEntity> listByUser(User user) {
        return fileRepository.findByUploaderOrderByCreatedAtDesc(user);
    }

    public Resource loadAsResource(Long id, User user) throws IOException {
        Optional<FileEntity> opt = fileRepository.findById(id);
        if (opt.isEmpty()) throw new IOException("File not found");
        FileEntity fe = opt.get();
        if (!fe.getUploader().getId().equals(user.getId())) {
            throw new IOException("Access denied");
        }
        Path filePath = Paths.get(fe.getStoragePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("Could not read file: " + fe.getStoredFilename());
            }
        } catch (MalformedURLException e) {
            throw new IOException("Malformed file path", e);
        }
    }

    public void delete(Long id, User user) throws IOException {
        Optional<FileEntity> opt = fileRepository.findById(id);
        if (opt.isEmpty()) throw new IOException("File not found");
        FileEntity fe = opt.get();
        if (!fe.getUploader().getId().equals(user.getId())) {
            throw new IOException("Access denied");
        }
        Path filePath = Paths.get(fe.getStoragePath());
        Files.deleteIfExists(filePath);
        fileRepository.delete(fe);
    }
}

