package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class FileStorageService {

    public FileStorageService() {
        // constructor injection can be added later if you need repositories, etc.
    }

    /**
     * Store the given multipart file on disk / DB and create a FileEntity.
     * TODO: implement storage, metadata persistence and return saved FileEntity.
     */
    public FileEntity storeFile(MultipartFile file, User owner) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Load a file as Resource for download. Should enforce owner access control.
     * TODO: implement resource loading (e.g., from filesystem or DB).
     */
    public Resource loadFile(Long id, User owner) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Delete the file identified by id. Should enforce owner access control.
     */
    public void deleteFile(Long id, User owner) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * List files belonging to the given owner.
     */
    public List<FileEntity> listFiles(User owner) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented");
    }
}
