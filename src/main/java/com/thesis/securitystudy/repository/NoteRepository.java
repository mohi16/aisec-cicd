// src/main/java/com/thesis/securitystudy/repository/NoteRepository.java
package com.thesis.securitystudy.repository;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByOwner(User owner);
    List<Note> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Note> findByIsPublicTrue();

    // Suche in eigenen Notizen (Title oder Content, partial, case-insensitive)
    List<Note> findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(
            User owner, String title,
            User owner2, String content);

    // Suche in öffentlichen Notizen (Title oder Content, partial, case-insensitive)
    List<Note> findByIsPublicTrueAndTitleContainingIgnoreCaseOrIsPublicTrueAndContentContainingIgnoreCase(
            String title, String content);
}