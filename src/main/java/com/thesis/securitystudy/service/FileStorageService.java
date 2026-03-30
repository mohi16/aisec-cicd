package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public FileStorageService() throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
    }

    public String saveFile(MultipartFile file, String storedFilename) throws IOException {
        Path filePath = uploadDir.resolve(storedFilename);
        file.transferTo(filePath);
        return filePath.toString();
    }

    public File loadFile(String storedFilename) {
        return uploadDir.resolve(storedFilename).toFile();
    }

    public void deleteFile(String storedFilename) throws IOException {
        Files.deleteIfExists(uploadDir.resolve(storedFilename));
    }
}
