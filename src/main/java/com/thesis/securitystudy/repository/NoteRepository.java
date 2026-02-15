package com.thesis.securitystudy.repository;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByOwner(User owner);
    List<Note> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Note> findByIsPublicTrue();

    // Search notes belonging to a specific owner by title or content (partial, case-insensitive)
    @Query("SELECT n FROM Note n WHERE n.owner = :owner AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY n.createdAt DESC")
    List<Note> searchByOwnerAndQuery(@Param("owner") User owner, @Param("q") String q);

    // Search public notes by title or content (partial, case-insensitive)
    @Query("SELECT n FROM Note n WHERE n.isPublic = true AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY n.createdAt DESC")
    List<Note> searchPublicByQuery(@Param("q") String q);
}
