package com.thesis.securitystudy.repository;

import com.thesis.securitystudy.model.FileEntity;
import com.thesis.securitystudy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUploader(User uploader);
    List<FileEntity> findByUploaderOrderByCreatedAtDesc(User uploader);
}
