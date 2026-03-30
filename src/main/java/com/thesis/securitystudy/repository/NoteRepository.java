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
    List<Note> findByOwnerAndTitleContainingIgnoreCaseOrOwnerAndContentContainingIgnoreCase(User owner, String titleQuery, User owner2, String contentQuery);

    @Query("SELECT n FROM Note n WHERE n.isPublic = true AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Note> searchPublicNotes(@Param("query") String query);

}

