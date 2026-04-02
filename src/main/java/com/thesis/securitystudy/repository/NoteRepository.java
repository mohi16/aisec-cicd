package com.thesis.securitystudy.repository;

import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import org.springframework.data.domain.Sort;
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

    // Search public notes (title OR content) case-insensitive, supports Sort param
    @Query("SELECT n FROM Note n " +
            "WHERE n.isPublic = true " +
            "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Note> searchPublic(@Param("q") String q, Sort sort);

    // Search notes visible to a specific user: either public OR owned by the user
    @Query("SELECT n FROM Note n " +
            "WHERE ((n.isPublic = true) OR (n.owner.id = :ownerId)) " +
            "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Note> searchVisibleToUser(@Param("ownerId") Long ownerId, @Param("q") String q, Sort sort);
}
